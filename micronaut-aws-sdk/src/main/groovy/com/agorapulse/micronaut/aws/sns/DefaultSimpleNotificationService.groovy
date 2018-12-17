package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.reactivex.Flowable
import io.reactivex.functions.Predicate

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@CompileStatic
class DefaultSimpleNotificationService implements SimpleNotificationService {

    private final AmazonSNS client
    private final SimpleNotificationServiceConfiguration configuration
    private final ObjectMapper objectMapper
    private final ConcurrentHashMap<String, String> namesToArn = new ConcurrentHashMap<>()

    DefaultSimpleNotificationService(AmazonSNS client, SimpleNotificationServiceConfiguration configuration, ObjectMapper objectMapper) {
        this.client = client
        this.configuration = configuration
        this.objectMapper = objectMapper
    }

    @Override
    String getAmazonApplicationArn() {
        return checkNotEmpty(configuration.amazon.arn, "Amazon application arn must be defined in config")
    }

    @Override
    String getAndroidApplicationArn() {
        return checkNotEmpty(configuration.android.arn, "Android application arn must be defined in config")
    }

    @Override
    String getIosApplicationArn() {
        return checkNotEmpty(configuration.ios.arn, "Ios application arn must be defined in config")
    }

    @Override
    String getDefaultTopicNameOrArn() {
        return checkNotEmpty(ensureTopicArn(configuration.topic), "Default topic not set for the configuration")
    }

    /**
     * @param topicName
     * @return
     */
    String createTopic(String topicName) {
        log.debug("Creating topic sns with name " + topicName)
        return client.createTopic(new CreateTopicRequest(topicName)).topicArn
    }

    /**
     * @param topicArn
     */
    void deleteTopic(String topicArn) {
        log.debug("Deleting topic " + topicArn)
        client.deleteTopic(new DeleteTopicRequest(ensureTopicArn(topicArn)))
    }

    /**
     * @param topicArn
     * @param subject
     * @param message
     * @return
     */
    String publishMessageToTopic(String topicArn, String subject, String message) {
        return client.publish(new PublishRequest(ensureTopicArn(topicArn), message, subject)).messageId
    }

    /**
     *
     * @param endpointArn
     * @param notification
     * @param collapseKey
     * @param delayWhileIdle
     * @param timeToLive
     * @param dryRun
     * @return
     */
    String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        publishToTarget(endpointArn, PLATFORM_TYPE_ANDROID, buildAndroidMessage(notification, collapseKey, delayWhileIdle, timeToLive, dryRun))
    }

    /**
     *
     * @param endpointArn
     * @param notification
     * @param sandbox
     * @return
     */
    String sendIosAppNotification(String endpointArn, Map notification, boolean sandbox) {
        publishToTarget(endpointArn, sandbox ? PLATFORM_TYPE_IOS_SANDBOX : PLATFORM_TYPE_IOS, buildIosMessage(notification))
    }

    /**
     *
     * @param phoneNumber
     * @param message
     * @param smsAttributes
     * @return
     */
    String sendSMSMessage(String phoneNumber, String message, Map smsAttributes) {
        client.publish(new PublishRequest().withMessage(message).withPhoneNumber(phoneNumber).withMessageAttributes(smsAttributes)).messageId
    }

    /**
     * @param topic
     * @param protocol
     * @param endpoint
     * @return
     */
    String subscribeTopic(String topic, String protocol, String endpoint) {
        log.debug("Creating a topic subscription to endpoint " + endpoint)
        client.subscribe(new SubscribeRequest(ensureTopicArn(topic), protocol, endpoint)).subscriptionArn
    }

    /**
     * @param arn
     */
    void unsubscribeTopic(String arn) {
        log.debug("Deleting a topic subscription to number " + arn)
        client.unsubscribe(new UnsubscribeRequest(arn))
    }

    /**
     *
     * @param endpointArn
     */
    void unregisterDevice(String endpointArn) {
        deleteEndpoint(endpointArn)
    }

    @Override
    String createPlatformApplication(String name, String platform, String principal, String credential) {
        CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()
            .withName(name)
            .withPlatform(platform)

        Map<String, String> attributes = [:]

        if (principal) {
            attributes['PlatformPrincipal'] = principal
        }

        if (credential) {
            attributes['PlatformCredential'] = credential
        }

        if (attributes) {
            request.withAttributes(attributes)
        }

        return client.createPlatformApplication(request).platformApplicationArn
    }

    String createPlatformEndpoint(String platformApplicationArn, String deviceToken, String customUserData) {
        try {
            log.debug("Creating platform endpoint with token " + deviceToken)
            CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(platformApplicationArn)
                .withToken(deviceToken)
            if (customUserData) {
                request.customUserData = customUserData
            }
            return client.createPlatformEndpoint(request).endpointArn
        } catch (InvalidParameterException ipe) {
            String message = ipe.errorMessage
            log.debug("Exception message: " + message)
            Pattern p = Pattern.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists with the same Token.*")
            Matcher m = p.matcher(message)
            if (m.matches()) {
                // The platform endpoint already exists for this token, but with additional custom data that
                // createEndpoint doesn't want to overwrite. Just use the existing platform endpoint.
                return m.group(1)
            }
            // Rethrow the exception, the input is actually bad.
            throw ipe
        }
    }

    private String buildAndroidMessage(Map data, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        objectMapper.writeValueAsString([
            collapse_key    : collapseKey,
            data            : data,
            delay_while_idle: delayWhileIdle,
            time_to_live    : timeToLive,
            dry_run         : dryRun
        ])
    }

    private String buildIosMessage(Map data) {
        objectMapper.writeValueAsString([
            aps: data
        ])
    }

    private DeleteEndpointResult deleteEndpoint(String endpointArn) {
        DeleteEndpointRequest depReq = new DeleteEndpointRequest()
            .withEndpointArn(endpointArn)
        client.deleteEndpoint(depReq)
    }


    private SetEndpointAttributesResult setEndpointAttributes(String endpointArn,
                                                              Map attributes) {
        SetEndpointAttributesRequest saeReq = new SetEndpointAttributesRequest()
            .withEndpointArn(endpointArn)
            .withAttributes(attributes)
        client.setEndpointAttributes(saeReq)
    }

    private String publishToTarget(String endpointArn, String platform, String message) {
        PublishRequest request = new PublishRequest(
            message: objectMapper.writeValueAsString([(platform): message]),
            messageStructure: 'json',
            targetArn: endpointArn, // For direct publish to mobile end points, topicArn is not relevant.
        )

        client.publish(request).messageId
    }

    String validatePlatformDeviceToken(String platformApplicationArn, String platformType, String endpointArn, String deviceToken, String customUserData = '') {
        log.debug "Retrieving platform endpoint data..."
        // Look up the platform endpoint and make sure the data in it is current, even if it was just created.
        try {
            GetEndpointAttributesResult result = client.getEndpointAttributes(new GetEndpointAttributesRequest().withEndpointArn(endpointArn))
            if (result.attributes.get("Token").equals(deviceToken) && result.attributes.get("Enabled").equalsIgnoreCase("true")) {
                return endpointArn
            }
        } catch (NotFoundException ignored) {
            // We had a stored ARN, but the platform endpoint associated with it disappeared. Recreate it.
            return createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
        }

        log.debug "Platform endpoint update required..."
        if (
        (platformType == MOBILE_PLATFORM_IOS && endpointArn.contains(PLATFORM_TYPE_ANDROID))
            ||
            (platformType == MOBILE_PLATFORM_ANDROID && endpointArn.contains(PLATFORM_TYPE_IOS))
        ) {
            log.debug "Switching between IOS and ANDROID platforms..."
            // Manager switched device between and android and an IOS device
            deleteEndpoint(endpointArn)
            return createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
        }

        // The platform endpoint is out of sync with the current data, update the token and enable it.
        log.debug("Updating platform endpoint " + endpointArn)
        try {
            setEndpointAttributes(endpointArn, [
                Token  : deviceToken,
                Enabled: 'true'
            ])
            return endpointArn
        } catch (InvalidParameterException ignored) {
            deleteEndpoint(endpointArn)
            return createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
        }

    }

    Flowable<Topic> listTopics() {
        FlowableListTopicHelper.generateTopics(client)
    }

    private static String checkNotEmpty(String arn, String errorMessage) {
        if (!arn) {
            throw new IllegalStateException(errorMessage)
        }
        return arn
    }

    private String ensureTopicArn(String nameOrArn) {
        if (!nameOrArn) {
            return ''
        }
        if (nameOrArn.startsWith('arn:aws:sns')) {
            return nameOrArn
        }

        if (namesToArn.contains(nameOrArn)) {
            return namesToArn[nameOrArn]
        }

        listTopics().takeUntil({ Topic topic -> topic.topicArn.endsWith(':' + nameOrArn) } as Predicate<Topic>).subscribe { Topic topic ->
            String topicName = topic.topicArn.substring(topic.topicArn.lastIndexOf(':') + 1)
            namesToArn[topicName] = topic.topicArn
        }

        String topicArn = namesToArn[nameOrArn]

        if (topicArn) {
            return topicArn
        }

        return createTopic(nameOrArn)
    }

}
