package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.DeleteEndpointResponse;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultSimpleNotificationService implements SimpleNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSimpleNotificationService.class);

    private final Map<String, String> namesToArn = new ConcurrentHashMap<>();
    private final SnsClient client;
    private final SimpleNotificationServiceConfiguration configuration;
    private final ObjectMapper objectMapper;

    public DefaultSimpleNotificationService(SnsClient client, SimpleNotificationServiceConfiguration configuration, ObjectMapper objectMapper) {
        this.client = client;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getAmazonApplicationArn() {
        return checkNotEmpty(configuration.getAmazon().getArn(), "Amazon application arn must be defined in config");
    }

    @Override
    public String getAndroidApplicationArn() {
        return checkNotEmpty(configuration.getAmazon().getArn(), "Android application arn must be defined in config");
    }

    @Override
    public String getIosApplicationArn() {
        return checkNotEmpty(configuration.getIos().getArn(), "Ios application arn must be defined in config");
    }

    @Override
    public String getIosSandboxApplicationArn() {
        return checkNotEmpty(configuration.getIosSandbox().getArn(), "Ios sandbox application arn must be defined in config");
    }

    @Override
    public String getDefaultTopicNameOrArn() {
        return checkNotEmpty(ensureTopicArn(configuration.getTopic()), "Default topic not set for the configuration");
    }

    @Override
    public String createTopic(String topicName) {
        LOGGER.debug("Creating topic sns with name " + topicName);
        return client.createTopic(b -> b.name(topicName)).topicArn();
    }

    @Override
    public Flowable<Topic> listTopics() {
        return Flowable.generate(client::listTopics, (BiFunction<ListTopicsResponse, Emitter<List<Topic>>, ListTopicsResponse>) (listTopicsResult, topicEmitter) -> {
            topicEmitter.onNext(listTopicsResult.topics());

            if (listTopicsResult.nextToken() != null) {
                return client.listTopics(r -> r.nextToken(listTopicsResult.nextToken()));
            }

            topicEmitter.onComplete();

            return null;
        }).flatMap(Flowable::fromIterable);
    }

    @Override
    public void deleteTopic(String topicArn) {
        LOGGER.debug("Deleting topic" + topicArn);
        client.deleteTopic(b -> b.topicArn(ensureTopicArn(topicArn)));
    }

    @Override
    public String subscribeTopic(String topic, String protocol, String endpoint) {
        LOGGER.debug("Creating a topic subscription to endpoint " + endpoint);
        return client.subscribe(b -> b.topicArn(ensureTopicArn(topic)).protocol(protocol).endpoint(endpoint)).subscriptionArn();
    }

    @Override
    public void unsubscribeTopic(String arn) {
        LOGGER.debug("Deleting a topic subscription to number " + arn);
        client.unsubscribe(b -> b.subscriptionArn(arn));
    }

    @Override
    public String publishMessageToTopic(String topicArn, String subject, String message) {
        return client.publish(r -> r.topicArn(ensureTopicArn(topicArn)).message(message).subject(subject)).messageId();
    }

    @Override
    public String createPlatformApplication(String name, String platformType, String principal, String credential) {
        return client.createPlatformApplication(r -> {
            r.name(name).platform(platformType);

            Map<String, String> attributes = new HashMap<>();

            if (principal != null && !principal.isEmpty()) {
                attributes.put("PlatformPrincipal", principal);
            }

            if (credential != null && !credential.isEmpty()) {
                attributes.put("PlatformCredential", credential);
            }

            if (!attributes.isEmpty()) {
                r.attributes(attributes);
            }

        }).platformApplicationArn();
    }

    @Override
    public String createPlatformEndpoint(String platformApplicationArn, String deviceToken, String customUserData) {
        try {
            LOGGER.debug("Creating platform endpoint with token " + deviceToken);
            CreatePlatformEndpointRequest.Builder request = CreatePlatformEndpointRequest.builder()
                .platformApplicationArn(platformApplicationArn)
                .token(deviceToken);
            if (customUserData != null && !customUserData.isEmpty()) {
                request.customUserData(customUserData);
            }
            return client.createPlatformEndpoint(request.build()).endpointArn();
        } catch (InvalidParameterException ipe) {
            String message = ipe.getMessage();
            LOGGER.debug("Exception message: " + message);
            Pattern p = Pattern.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same Token.*");
            Matcher m = p.matcher(message);
            if (m.matches()) {
                // The platform endpoint already exists for this token, but with additional custom data that
                // createEndpoint doesn't want to overwrite. Just use the existing platform endpoint.
                return m.group(1);
            }
            // Rethrow the exception, the input is actually bad.
            throw ipe;
        }
    }

    @Override
    public String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        return publishToTarget(endpointArn, PLATFORM_TYPE_ANDROID, buildAndroidMessage(notification, collapseKey, delayWhileIdle, timeToLive, dryRun));
    }

    @Override
    public String sendIosAppNotification(String endpointArn, Map<String, Object> notification, boolean sandbox) {
        return publishToTarget(endpointArn, sandbox ? PLATFORM_TYPE_IOS_SANDBOX : PLATFORM_TYPE_IOS, buildIosMessage(notification));
    }

    @Override
    public String validateDeviceToken(String platformApplicationArn, String endpointArn, String deviceToken, String customUserData) {
        LOGGER.debug("Retrieving platform endpoint data...");
        // Look up the platform endpoint and make sure the data in it is current, even if it was just created.
        try {
            GetEndpointAttributesResponse result = client.getEndpointAttributes(r -> r.endpointArn(endpointArn));
            if (Objects.equals(result.attributes().get("Token"), deviceToken) && result.attributes().get("Enabled").equalsIgnoreCase(Boolean.TRUE.toString())) {
                setEndpointAttributes(endpointArn, Collections.singletonMap("CustomUserData", customUserData));
                return endpointArn;
            }
        } catch (NotFoundException ignored) {
            // We had a stored ARN, but the platform endpoint associated with it disappeared. Recreate it.
            return createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData);
        }

        LOGGER.debug("Platform endpoint update required...");

        // The platform endpoint is out of sync with the current data, update the token and enable it.
        LOGGER.debug("Updating platform endpoint " + endpointArn);
        try {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("CustomUserData",  customUserData);
            attrs.put("Enabled",  Boolean.TRUE.toString());
            setEndpointAttributes(endpointArn, attrs);
            return endpointArn;
        } catch (InvalidParameterException ignored) {
            deleteEndpoint(endpointArn);
            return createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData);
        }
    }

    @Override
    public void unregisterDevice(String endpointArn) {
        deleteEndpoint(endpointArn);
    }

    @Override
    public String sendSMSMessage(String phoneNumber, String message, Map<String, MessageAttributeValue> smsAttributes) {
        return client.publish(r -> r.message(message).phoneNumber(phoneNumber).messageAttributes(smsAttributes)).messageId();
    }

    private static String checkNotEmpty(String arn, String errorMessage) {
        if (arn == null || arn.isEmpty()) {
            throw new IllegalStateException(errorMessage);
        }
        return arn;
    }

    private String ensureTopicArn(String nameOrArn) {
        if (nameOrArn == null || nameOrArn.isEmpty()) {
            return "";
        }
        if (nameOrArn.startsWith("arn:aws:sns")) {
            return nameOrArn;
        }

        if (namesToArn.containsKey(nameOrArn)) {
            return namesToArn.get(nameOrArn);
        }

        listTopics()
            .takeUntil((Topic topic) -> topic.topicArn().endsWith(":" + nameOrArn))
            .subscribe(topic -> {
                String topicName = topic.topicArn().substring(topic.topicArn().lastIndexOf(':') + 1);
                namesToArn.put(topicName, topic.topicArn());
            });

        String topicArn = namesToArn.get(nameOrArn);

        if (topicArn != null && !topicArn.isEmpty()) {
            return topicArn;
        }

        return createTopic(nameOrArn);
    }

    private String buildAndroidMessage(Map<String, Object> data, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("collapse_key", collapseKey);
        value.put("data", data);
        value.put("delay_while_idle", delayWhileIdle);
        value.put("time_to_live", timeToLive);
        value.put("dry_run", dryRun);
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write android message " + value, e);
        }
    }

    private String buildIosMessage(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(Collections.singletonMap("aps", data));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write iOS message with data" + data, e);
        }
    }

    private String publishToTarget(String endpointArn, String platformType, String message) {
        return client.publish(r -> {
            try {
                r.targetArn(endpointArn).messageStructure("json").message(objectMapper.writeValueAsString(Collections.singletonMap(platformType, message)));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot write json for platform " + platformType + " and message " + message , e);
            }
        }).messageId();
    }

    private SetEndpointAttributesResponse setEndpointAttributes(String endpointArn, Map<String, String> attributes) {
        return client.setEndpointAttributes(r -> r.endpointArn(endpointArn).attributes(attributes));
    }


    private DeleteEndpointResponse deleteEndpoint(String endpointArn) {
        return client.deleteEndpoint(r -> r.endpointArn(endpointArn));
    }
}
