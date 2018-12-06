package com.agorapulse.micronaut.aws.sns;

import java.util.Map;

public interface SimpleNotificationService {

    String MOBILE_PLATFORM_ANDROID = "android";
    String MOBILE_PLATFORM_IOS = "ios";


    /**
     * @param topicName
     * @return
     */
    String createTopic(String topicName);

    /**
     * @param topicArn
     */
    void deleteTopic(String topicArn);

    /**
     * @param topicArn
     * @param subject
     * @param message
     * @return
     */
    String publishTopic(String topicArn, String subject, String message);

    /**
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String registerAndroidDevice(String deviceToken, String customUserData);

    /**
     * @param deviceToken
     * @return
     */
    default String registerAndroidDevice(String deviceToken) {
        return registerAndroidDevice(deviceToken, "");
    }

    String createPlatformEndpoint(String platformApplicationArn, String deviceToken, String customUserData);

    default String createPlatformEndpoint(String platformApplicationArn, String deviceToken) {
        return createPlatformEndpoint(platformApplicationArn, deviceToken);
    }



    /**
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String registerIosDevice(String deviceToken, String customUserData);

    /**
     * @param deviceToken
     * @return
     */
    default String registerIosDevice(String deviceToken) {
        return registerIosDevice(deviceToken, "");
    }

    /**
     * @param platformType
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String registerDevice(String platformType, String deviceToken, String customUserData) {
        if (MOBILE_PLATFORM_ANDROID.equals(platformType)) {
            registerAndroidDevice(deviceToken, customUserData);
        } else if (MOBILE_PLATFORM_IOS.equals(platformType)) {
            registerIosDevice(deviceToken, customUserData);
        }
        return null;
    }

    /**
     * @param platformType
     * @param deviceToken
     * @return
     */
    default String registerDevice(String platformType, String deviceToken) {
        return registerDevice(platformType, deviceToken, "");
    }

    /**
     * @param endpointArn
     * @param notification
     * @param collapseKey
     * @param delayWhileIdle
     * @param timeToLive
     * @param dryRun
     * @return
     */
    String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun);

    /**
     * @param endpointArn
     * @param notification
     * @param collapseKey
     * @param delayWhileIdle
     * @param timeToLive
     * @return
     */
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle, int timeToLive) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, timeToLive, false);
    }

    /**
     * @param endpointArn
     * @param notification
     * @param collapseKey
     * @param delayWhileIdle
     * @return
     */
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, 125);
    }

    /**
     * @param endpointArn
     * @param notification
     * @param collapseKey
     * @return
     */
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, true);
    }

    /**
     * @param endpointArn
     * @param notification
     * @param sandbox
     * @return
     */
    String sendIosAppNotification(String endpointArn, Map notification, boolean sandbox);

    /**
     * @param endpointArn
     * @param notification
     * @return
     */
    default String sendIosAppNotification(String endpointArn, Map notification) {
        return sendIosAppNotification(endpointArn, notification, false);
    }

    /**
     * @param phoneNumber
     * @param message
     * @param smsAttributes
     * @return
     */
    String sendSMSMessage(String phoneNumber, String message, Map smsAttributes);

    /**
     * @param topic
     * @param protocol
     * @param endpoint
     * @return
     */
    String subscribeTopic(String topic, String protocol, String endpoint);

    /**
     * @param topicArn
     * @param number
     * @return
     */
    String subscribeTopicWithSMS(String topicArn, String number);

    /**
     * @param arn
     */
    void unsubscribeTopic(String arn);

    /**
     * @param endpointArn
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String validateAndroidDevice(String endpointArn, String deviceToken, String customUserData);

    /**
     * @param endpointArn
     * @param deviceToken
     * @return
     */
    default String validateAndroidDevice(String endpointArn, String deviceToken) {
        return validateAndroidDevice(endpointArn, deviceToken, "");
    }

    /**
     * @param endpointArn
     * @param deviceToken
     * @param customUserData
     * @return
     */
    String validateIosDevice(String endpointArn, String deviceToken, String customUserData);

    /**
     * @param endpointArn
     * @param deviceToken
     * @return
     */
    default String validateIosDevice(String endpointArn, String deviceToken) {
        return validateIosDevice(endpointArn, deviceToken, "");
    }

    /**
     * @param platformType
     * @param endpointArn
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String validateDevice(String platformType, String endpointArn, String deviceToken, String customUserData) {
        if (MOBILE_PLATFORM_ANDROID.equals(platformType)) {
            validateAndroidDevice(endpointArn, deviceToken, customUserData);
        } else if (MOBILE_PLATFORM_IOS.equals(platformType)) {
            validateIosDevice(endpointArn, deviceToken, customUserData);
        }
        return null;
    }

    /**
     * @param platformType
     * @param endpointArn
     * @param deviceToken
     * @return
     */
    default String validateDevice(String platformType, String endpointArn, String deviceToken) {
        return validateDevice(platformType, endpointArn, deviceToken, "");
    }

    /**
     * @param endpointArn
     */
    void unregisterDevice(String endpointArn);
}
