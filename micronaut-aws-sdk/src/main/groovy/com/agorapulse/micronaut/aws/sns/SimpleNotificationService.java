package com.agorapulse.micronaut.aws.sns;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public interface SimpleNotificationService {

    String MOBILE_PLATFORM_ANDROID = "android";
    String MOBILE_PLATFORM_IOS = "ios";
    String MOBILE_PLATFORM_AMAZON = "amazon";
    String PLATFORM_TYPE_IOS_SANDBOX = "APNS_SANDBOX";
    String PLATFORM_TYPE_IOS = "APNS";
    String PLATFORM_TYPE_ANDROID = "GCM";
    String PLATFORM_TYPE_AMAZON = "ADM";


    String getAmazonApplicationArn();

    String getAndroidApplicationArn();

    String getIosApplicationArn();

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
    String publishMessageToTopic(String topicArn, String subject, String message);

    /**
     * @param platformType
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String registerDevice(String platformType, String deviceToken, String customUserData) {
        if (MOBILE_PLATFORM_ANDROID.equals(platformType)) {
            return registerAndroidDevice(deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS.equals(platformType)) {
            return registerIosDevice(deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_AMAZON.equals(platformType)) {
            return registerAmazonDevice(deviceToken, customUserData);
        }
        throw new IllegalArgumentException("Platform " + platformType + " is not supported");
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
     * @param deviceToken
     * @return
     */
    default String registerAndroidDevice(String deviceToken) {
        return registerAndroidDevice(deviceToken, "");
    }

    /**
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String registerAndroidDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getAndroidApplicationArn(), deviceToken, customUserData);
    }

    default String registerIosDevice(String deviceToken) {
        return registerIosDevice(deviceToken, "");
    }

    /**
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String registerIosDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getIosApplicationArn(), deviceToken, customUserData);
    }

    default String registerAmazonDevice(String deviceToken) {
        return registerIosDevice(deviceToken, "");
    }

    /**
     * @param deviceToken
     * @param customUserData
     * @return
     */
    default String registerAmazonDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getAmazonApplicationArn(), deviceToken, customUserData);
    }

    String createPlatformEndpoint(String platformApplicationArn, String deviceToken, String customUserData);

    default String createPlatformEndpoint(String platformApplicationArn, String deviceToken) {
        return createPlatformEndpoint(platformApplicationArn, deviceToken, null);
    }

    String createPlatformApplication(String name, String platform, String principal, String credential);

    default String createIosApplication(String name, String privateKey, String sslCertificate, boolean sandbox) {
        return createPlatformApplication(name, sandbox ? PLATFORM_TYPE_IOS_SANDBOX : PLATFORM_TYPE_IOS, sslCertificate, privateKey);
    }

    default String createAndroidApplication(String name, String apiKey) {
        return createPlatformApplication(name, PLATFORM_TYPE_ANDROID, null, apiKey);
    }

    default String createAmazonApplication(String name, String clientId, String clientSecret) {
        return createPlatformApplication(name, PLATFORM_TYPE_AMAZON, clientId, clientSecret);
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
     * Generic version of subscribing to the topic.
     * <p>
     * It is recommended to use one of the specialised methods instead.
     *
     * @param topic    ARN of the topic
     * @param protocol see {@link com.amazonaws.services.sns.model.SubscribeRequest}
     * @param endpoint see {@link com.amazonaws.services.sns.model.SubscribeRequest}
     * @return subscription ARN
     * @see #subscribeTopicWithApplication(String, String)
     * @see #subscribeTopicWithEndpoint(String, String)
     * @see #subscribeTopicWithEndpoint(String, URL)
     * @see #subscribeTopicWithEmail(String, String)
     * @see #subscribeTopicWithJsonEmail(String, String)
     * @see #subscribeTopicWithQueue(String, String)
     * @see #subscribeTopicWithSMS(String, String)
     * @see #subscribeTopicWithFunction(String, String)
     * @see com.amazonaws.services.sns.model.SubscribeRequest
     */
    String subscribeTopic(String topic, String protocol, String endpoint);

    default String subscribeTopicWithEndpoint(String topicArn, String url) throws MalformedURLException {
        return subscribeTopicWithEndpoint(topicArn, new URL(url));
    }

    default String subscribeTopicWithEndpoint(String topicArn, URL url) {
        if ("https".equals(url.getProtocol())) {
            return subscribeTopic(topicArn, "https", url.toExternalForm());
        }
        if ("http".equals(url.getProtocol())) {
            return subscribeTopic(topicArn, "http", url.toExternalForm());
        }
        throw new IllegalArgumentException("Can only subscribe to HTTP or HTTPS endpoints!");
    }

    default String subscribeTopicWithEmail(String topicArn, String email) {
        return subscribeTopic(topicArn, "email", email);
    }

    default String subscribeTopicWithJsonEmail(String topicArn, String email) {
        return subscribeTopic(topicArn, "email-json", email);
    }

    default String subscribeTopicWithSMS(String topicArn, String number) {
        return subscribeTopic(topicArn, "sms", number);
    }

    default String subscribeTopicWithQueue(String topicArn, String queueArn) {
        return subscribeTopic(topicArn, "sqs", queueArn);
    }

    default String subscribeTopicWithApplication(String topicArn, String applicationEndpointArn) {
        return subscribeTopic(topicArn, "application", applicationEndpointArn);
    }

    default String subscribeTopicWithFunction(String topicArn, String lambdaArn) {
        return subscribeTopic(topicArn, "lambda", lambdaArn);
    }

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
    default String validateAndroidDevice(String endpointArn, String deviceToken, String customUserData) {
        return validatePlatformDeviceToken(getAndroidApplicationArn(), MOBILE_PLATFORM_ANDROID, endpointArn, deviceToken, customUserData);
    }

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
    default String validateIosDevice(String endpointArn, String deviceToken, String customUserData) {
        return validatePlatformDeviceToken(getIosApplicationArn(), MOBILE_PLATFORM_IOS, endpointArn, deviceToken, customUserData);
    }

    String validatePlatformDeviceToken(String iosApplicationArn, String mobilePlatform, String endpointArn, String deviceToken, String customUserData);

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
            return validateAndroidDevice(endpointArn, deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS.equals(platformType)) {
            return validateIosDevice(endpointArn, deviceToken, customUserData);
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
