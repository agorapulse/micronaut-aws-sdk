/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteEndpointResponse;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
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
import java.util.stream.Collectors;

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
    public String getPlatformApplicationArn(PlatformType platformType) {
        switch (platformType) {
            case ADM:
                return checkNotEmpty(configuration.getAdm().getArn(), "Amazon Device Manager application arn must be defined in config");
            case APNS:
                return checkNotEmpty(configuration.getApns().getArn(), "Apple Push Notification service application arn must be defined in config");
            case APNS_SANDBOX:
                return checkNotEmpty(configuration.getApnsSandbox().getArn(), "Apple Push Notification service Sandbox application arn must be defined in config");
            case GCM:
                return checkNotEmpty(configuration.getGcm().getArn(), "Google Cloud Messaging (Firebase) application arn must be defined in config");
            default:
                throw new IllegalArgumentException("Unknown platform type " + platformType);
        }
    }

    @Override
    @Deprecated
    public String getAmazonApplicationArn() {
        return checkNotEmpty(configuration.getAmazon().getArn(), "Amazon application arn must be defined in config");
    }

    @Override
    @Deprecated
    public String getAndroidApplicationArn() {
        return checkNotEmpty(configuration.getAmazon().getArn(), "Android application arn must be defined in config");
    }

    @Override
    @Deprecated
    public String getIosApplicationArn() {
        return checkNotEmpty(configuration.getIos().getArn(), "Ios application arn must be defined in config");
    }

    @Override
    @Deprecated
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
        return client.createTopic((CreateTopicRequest.Builder b) ->
             b.name(topicName).attributes(Collections.singletonMap("FifoTopic", Boolean.toString(SimpleNotificationService.isFifoTopic(topicName))))
        ).topicArn();
    }

    @Override
    public Publisher<Topic> listTopics() {
        return Flux.generate(client::listTopics, (ListTopicsResponse listTopicsResult, SynchronousSink<List<Topic>> sink) -> {
            sink.next(listTopicsResult.topics());

            if (listTopicsResult.nextToken() != null) {
                try {
                    return client.listTopics(r -> r.nextToken(listTopicsResult.nextToken()));

                } catch (SdkClientException e) {
                    sink.error(e);
                }
            }

            sink.complete();

            return ListTopicsResponse.builder().build();
        }).flatMap(Flux::fromIterable);
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
    public String publishMessageToTopic(String topicArn, String subject, String message, Map<String, String> attributes) {
        return client.publish(r -> r.topicArn(ensureTopicArn(topicArn))
            .message(message)
            .subject(subject)
            .messageAttributes(toAttributes(attributes))
        ).messageId();
    }

    @Override
    public String publishRequest(String topicArn, Map<String, String> attributes, PublishRequest.Builder publishRequestBuilder) {
        return client.publish(publishRequestBuilder.topicArn(ensureTopicArn(topicArn))
            .messageAttributes(toAttributes(attributes))
            .build()
        ).messageId();
    }

    @Override
    public String createPlatformApplication(String name, PlatformType platformType, String principal, String credential) {
        return createPlatformApplication(name, platformType.toString(), principal, credential);
    }

    @Override
    @Deprecated
    public String createPlatformApplication(String name, String endpointName, String principal, String credential) {
        return client.createPlatformApplication(r -> {
            r.name(name).platform(endpointName);

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
    @Deprecated
    public String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        return publishToTarget(endpointArn, PlatformType.GCM.toString(), buildAndroidMessage(notification, collapseKey, delayWhileIdle, timeToLive, dryRun));
    }

    @Override
    @Deprecated
    public String sendIosAppNotification(String endpointArn, Map<String, Object> notification, boolean sandbox) {
        return publishToTarget(endpointArn, sandbox ? PlatformType.APNS_SANDBOX.toString() : PlatformType.APNS.toString(), buildIosMessage(notification));
    }

    public String sendNotification(String endpointArn, PlatformType platformType, String jsonMessage) {
        return publishToTarget(endpointArn, platformType.toString(), jsonMessage);
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

    private static Map<String, MessageAttributeValue> toAttributes(Map<String, String> attributes) {
        if (attributes.isEmpty()) {
            return Collections.emptyMap();
        }

        return attributes.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> MessageAttributeValue.builder().stringValue(e.getValue()).dataType("String").build()
        ));
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

        Flux.from(listTopics())
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
        return toJson(value);
    }

    private String buildIosMessage(Map<String, Object> data) {
        return toJson(Collections.singletonMap("aps", data));
    }

    private String publishToTarget(String endpointArn, String platformType, String jsonMessage) {
        return client.publish(r -> {
            r.targetArn(endpointArn).messageStructure("json").message(toJson(Collections.singletonMap(platformType, jsonMessage)));
        }).messageId();
    }

    private SetEndpointAttributesResponse setEndpointAttributes(String endpointArn, Map<String, String> attributes) {
        return client.setEndpointAttributes(r -> r.endpointArn(endpointArn).attributes(attributes));
    }


    private DeleteEndpointResponse deleteEndpoint(String endpointArn) {
        return client.deleteEndpoint(r -> r.endpointArn(endpointArn));
    }

    private String toJson(Map<String, Object> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write json for message " + message, e);
        }
    }
}
