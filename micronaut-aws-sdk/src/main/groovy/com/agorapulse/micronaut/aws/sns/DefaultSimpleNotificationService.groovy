package com.agorapulse.micronaut.aws.sns


import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@CompileStatic
class DefaultSimpleNotificationService implements SimpleNotificationService {

    private static final String MOBILE_PLATFORM_ANDROID = 'android'
    private static final String MOBILE_PLATFORM_IOS = 'ios'

    private final AmazonSNS client
    private final SimpleNotificationServiceConfiguration configuration
    private final ObjectMapper objectMapper

    DefaultSimpleNotificationService(AmazonSNS client, SimpleNotificationServiceConfiguration configuration, ObjectMapper objectMapper) {
        this.client = client
        this.configuration = configuration
        this.objectMapper = objectMapper
    }

    /**
     * @param topicName
     * @return
     */
    String createTopic(String topicName){
        try {
            log.debug("Creating topic sns with name " + topicName)
            return client.createTopic(new CreateTopicRequest(topicName)).topicArn
        } catch (Exception e){
            log.error 'An exception was catched while creating a topic',  e
            return null
        }
    }

    /**
     * @param topicArn
     */
    void deleteTopic(String topicArn){
        try {
            log.debug("Deleting topic sns with arn " + topicArn)
            client.deleteTopic(new DeleteTopicRequest(topicArn))
        } catch (Exception e){
            log.error 'An exception was catched while creating a topic',  e
        }
    }

    /**
     * @param topicArn
     * @param subject
     * @param message
     * @return
     */
    String publishTopic(String topicArn, String subject, String message){
        try {
            PublishResult publishResult = client.publish(new PublishRequest(topicArn, message,subject));
            return publishResult.messageId
        } catch (Exception e){
            log.error 'An exception was catched while publishing',  e
            return null
        }
    }


    /**
     *
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String registerAndroidDevice(String deviceToken, String customUserData) {
        String platformApplicationArn = configuration.android.arn
        assert platformApplicationArn, 'Android application arn must be defined in config'
        createPlatformEndpoint(
                platformApplicationArn,
                deviceToken,
                customUserData
        )
    }

    /**
     *
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String registerIosDevice(String deviceToken, String customUserData) {
        String platformApplicationArn = configuration.ios.arn
        assert platformApplicationArn, 'Ios application arn must be defined in config'
        createPlatformEndpoint(
                platformApplicationArn,
                deviceToken,
                customUserData
        )
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
        String message = buildAndroidMessage(
                notification,
                collapseKey,
                delayWhileIdle,
                timeToLive,
                dryRun
        )
        publish(
                endpointArn,
                'GCM',
                message
        )
    }

    /**
     *
     * @param endpointArn
     * @param notification
     * @param sandbox
     * @return
     */
    String sendIosAppNotification(String endpointArn, Map notification, boolean sandbox) {
        String message = buildIosMessage(notification)
        publish(endpointArn, sandbox ? 'APNS_SANDBOX' : 'APNS', message)
    }

    /**
     *
     * @param phoneNumber
     * @param message
     * @param smsAttributes
     * @return
     */
    String sendSMSMessage(String phoneNumber, String message, Map smsAttributes) {
        PublishResult result
        try {
            result = client.publish(new PublishRequest()
                    .withMessage(message)
                    .withPhoneNumber(phoneNumber)
                    .withMessageAttributes(smsAttributes))
        } catch (Exception e){
            log.error 'An exception was catched while publishing',  e
        }

        result ? result.messageId : ''
    }

    /**
     * @param topic
     * @param protocol
     * @param endpoint
     * @return
     */
    String subscribeTopic(String topic, String protocol, String endpoint){

        log.debug("Creating a topic subscription to endpoit " + endpoint)
        SubscribeRequest subRequest = new SubscribeRequest(topic, protocol, endpoint);

        try {
            SubscribeResult result = client.subscribe(subRequest);
            result.subscriptionArn
        } catch (Exception e){
            log.error 'An exception was catched while subscribe a topic',  e
        }
    }

    /**
     * @param topicArn
     * @param number
     * @return
     */
    String subscribeTopicWithSMS(String topicArn, String number){
        try {
            subscribeTopic(topicArn,'sms',number)
        } catch (Exception e){
            log.error 'An exception was catched while subscribe a topic with sms protocol',  e
        }
    }

    /**
     * @param arn
     */
    void unsubscribeTopic(String arn){
        log.debug("Deleting a topic subscription to number " + arn)
        UnsubscribeRequest unSubRequest = new UnsubscribeRequest(arn);

        try {
            client.unsubscribe(unSubRequest);
        } catch (Exception e){
            log.error 'An exception was catched while unsubscribe a topic',  e
        }
    }

    /**
     *
     * @param endpointArn
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String validateAndroidDevice(String endpointArn, String deviceToken, String customUserData) {
        String platformApplicationArn = configuration.android.arn
        assert platformApplicationArn, 'Android application arn must be defined in config'
        validatePlatformDeviceToken(platformApplicationArn, MOBILE_PLATFORM_ANDROID, endpointArn, deviceToken, customUserData)
    }

    /**
     *
     * @param endpointArn
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String validateIosDevice(String endpointArn, String deviceToken, String customUserData) {
        String platformApplicationArn = configuration.ios.arn
        assert platformApplicationArn, 'Ios application arn must be defined in config'
        validatePlatformDeviceToken(
                platformApplicationArn,
                MOBILE_PLATFORM_IOS,
                endpointArn,
                deviceToken,
                customUserData
        )
    }

    /**
     *
     * @param endpointArn
     */
    void unregisterDevice(String endpointArn) {
        deleteEndpoint(endpointArn)
    }

    private String buildAndroidMessage(Map data, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun) {
        objectMapper.writeValueAsString([
                collapse_key: collapseKey,
                data: data,
                delay_while_idle: delayWhileIdle,
                time_to_live: timeToLive,
                dry_run: dryRun
        ])
    }

    private String buildIosMessage(Map data) {
        objectMapper.writeValueAsString([
                aps: data
        ])
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

    private DeleteEndpointResult deleteEndpoint(String endpointArn) {
        DeleteEndpointRequest depReq = new DeleteEndpointRequest()
                .withEndpointArn(endpointArn)
        client.deleteEndpoint(depReq)
    }

    private GetEndpointAttributesResult getEndpointAttributes(String endpointArn) {
        GetEndpointAttributesRequest geaReq = new GetEndpointAttributesRequest().withEndpointArn(endpointArn)
        client.getEndpointAttributes(geaReq)
    }

    private SetEndpointAttributesResult setEndpointAttributes(String endpointArn,
                                                              Map attributes) {
        SetEndpointAttributesRequest saeReq = new SetEndpointAttributesRequest()
                .withEndpointArn(endpointArn)
                .withAttributes(attributes)
        client.setEndpointAttributes(saeReq)
    }

    private String publish(String endpointArn,
                           String platform,
                           String message) {
        PublishRequest request = new PublishRequest(
                message: objectMapper.writeValueAsString([(platform): message]),
                messageStructure: 'json',
                targetArn: endpointArn, // For direct publish to mobile end points, topicArn is not relevant.
        )

        PublishResult result = null
        try {
            result = client.publish(request)
        } catch (Exception e) {
            log.error 'An exception was catched while publishing',  e
        }
        result ? result.messageId : ''
    }

    private String validatePlatformDeviceToken(String platformApplicationArn,
                                               String platformType,
                                               String endpointArn,
                                               String deviceToken,
                                               String customUserData = '') {
        boolean updateRequired = false

        log.debug "Retrieving platform endpoint data..."
        // Look up the platform endpoint and make sure the data in it is current, even if it was just created.
        try {
            GetEndpointAttributesResult result = getEndpointAttributes(endpointArn)
            updateRequired = !result.attributes.get("Token").equals(deviceToken) || !result.attributes.get("Enabled").equalsIgnoreCase("true")
        } catch (NotFoundException nfe) {
            // We had a stored ARN, but the platform endpoint associated with it disappeared. Recreate it.
            endpointArn = createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
        }

        if (updateRequired) {
            log.debug "Platform endpoint update required..."
            if ((platformType == MOBILE_PLATFORM_IOS && endpointArn.contains('GCM')) ||
                    (platformType == MOBILE_PLATFORM_ANDROID && endpointArn.contains('APNS'))) {
                log.debug "Switching between IOS and ANDROID platforms..."
                // Manager switched device between and android and an IOS device
                deleteEndpoint(endpointArn)
                endpointArn = createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
            } else {
                // The platform endpoint is out of sync with the current data, update the token and enable it.
                log.debug("Updating platform endpoint " + endpointArn)
                try {
                    setEndpointAttributes(endpointArn, [
                            Token: deviceToken,
                            Enabled: 'true'
                    ])
                } catch (InvalidParameterException ipe) {
                    deleteEndpoint(endpointArn)
                    endpointArn = createPlatformEndpoint(platformApplicationArn, deviceToken, customUserData)
                }
            }
        }
        endpointArn
    }

}
