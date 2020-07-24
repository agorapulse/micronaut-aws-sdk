/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.services.sns.model.Topic;
import io.reactivex.Flowable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Service to simplify interaction with Amazon SNS.
 */
@SuppressWarnings("rawtypes")
public interface SimpleNotificationService {

    @Deprecated
    String MOBILE_PLATFORM_ANDROID = "android";
    @Deprecated
    String MOBILE_PLATFORM_IOS = "ios";
    @Deprecated
    String MOBILE_PLATFORM_IOS_SANDBOX = "iosSandbox";
    @Deprecated
    String MOBILE_PLATFORM_AMAZON = "amazon";
    @Deprecated
    String PLATFORM_TYPE_IOS_SANDBOX = "APNS_SANDBOX";
    @Deprecated
    String PLATFORM_TYPE_IOS = "APNS";
    @Deprecated
    String PLATFORM_TYPE_ANDROID = "GCM";
    @Deprecated
    String PLATFORM_TYPE_AMAZON = "ADM";

    enum PLATFORM_TYPE {
        ADM,
        APNS,
        APNS_SANDBOX,
        GCM
    }

    /**
     * @param platform: target platform
     * @return the default Application ARN for given platform
     */
    String getPushPlatformArn(PLATFORM_TYPE platform);

    /**
     * deprecated : use getPushPlatformArn() with matching PUSH_PLATFORM
     * @return the default Application ARN for Amazon devices integration.
     */
    @Deprecated
    String getAmazonApplicationArn();

    /**
     * deprecated : use getPushPlatformArn() with matching PUSH_PLATFORM
     * @return the default Application ARN for Android devices integration.
     */
    @Deprecated
    String getAndroidApplicationArn();

    /**
     * deprecated : use getPushPlatformArn() with matching PUSH_PLATFORM
     * @return the default Application ARN for iOS devices integration.
     */
    @Deprecated
    String getIosApplicationArn();

    /**
     * deprecated : use getPushPlatformArn() with matching PUSH_PLATFORM
     * @return the default Application ARN for iOS sandbox devices integration.
     */
    @Deprecated
    String getIosSandboxApplicationArn();

    /**
     * @return the default topic ARN or name.
     */
    String getDefaultTopicNameOrArn();

    /**
     * Creates new topic with given name.
     * @param topicName topic name
     * @return ARN of newly created topic
     */
    String createTopic(String topicName);

    /**
     * @return flowable iterating through all the topics available
     */
    Flowable<Topic> listTopics();

    /**
     * Deletes given topic.
     * @param topicArn topic ARN or name
     */
    void deleteTopic(String topicArn);

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

    /**
     * Subscribe to topic with HTTP or HTTPS endpoint.
     * @param topicArn topic ARN or name
     * @param url the url to post new messages
     * @return subscription ARN
     */
    default String subscribeTopicWithEndpoint(String topicArn, String url) throws MalformedURLException {
        return subscribeTopicWithEndpoint(topicArn, new URL(url));
    }

    /**
     * Subscribe to topic with HTTP or HTTPS endpoint.
     * @param topicArn topic ARN or name
     * @param url the url to post new messages
     * @return subscription ARN
     */
    default String subscribeTopicWithEndpoint(String topicArn, URL url) {
        if ("https".equals(url.getProtocol())) {
            return subscribeTopic(topicArn, "https", url.toExternalForm());
        }
        if ("http".equals(url.getProtocol())) {
            return subscribeTopic(topicArn, "http", url.toExternalForm());
        }
        throw new IllegalArgumentException("Can only subscribe to HTTP or HTTPS endpoints!");
    }

    /**
     * Subscribes to the topic with an email.
     * @param topicArn topic ARN or name
     * @param email email to subscribe with
     * @return subscription ARN
     */
    default String subscribeTopicWithEmail(String topicArn, String email) {
        return subscribeTopic(topicArn, "email", email);
    }

    /**
     * Subscribes to the topic with JSON email.
     * @param topicArn topic ARN or name
     * @param email email to subscribe with
     * @return subscription ARN
     */
    default String subscribeTopicWithJsonEmail(String topicArn, String email) {
        return subscribeTopic(topicArn, "email-json", email);
    }

    /**
     * Subscribes to the topic with SMS to the phone number.
     * @param topicArn topic ARN or name
     * @param number phone number in international format
     * @return subscription ARN
     */
    default String subscribeTopicWithSMS(String topicArn, String number) {
        return subscribeTopic(topicArn, "sms", number);
    }

    /**
     * Subscribes to the topic with SQS queue.
     * @param topicArn topic ARN or name
     * @param queueArn ARN of the queue to be subscribed
     * @return subscription ARN
     */
    default String subscribeTopicWithQueue(String topicArn, String queueArn) {
        return subscribeTopic(topicArn, "sqs", queueArn);
    }

    /**
     * Subscribes to the topic with SNS application.
     * @param topicArn topic ARN or name
     * @param applicationEndpointArn ARN of the application to be subscribed
     * @return subscription ARN
     */
    default String subscribeTopicWithApplication(String topicArn, String applicationEndpointArn) {
        return subscribeTopic(topicArn, "application", applicationEndpointArn);
    }

    /**
     * Subscribes to the topic with Lambda function.
     * @param topicArn topic ARN or name
     * @param lambdaArn ARN of the lambda to be subscribed with
     * @return subscription ARN
     */
    default String subscribeTopicWithFunction(String topicArn, String lambdaArn) {
        return subscribeTopic(topicArn, "lambda", lambdaArn);
    }

    /**
     * Unsubscribes from the topic.
     * @param arn ARN of the subscription
     */
    void unsubscribeTopic(String arn);

    /**
     * Publishes a message into the topic.
     * @param topicArn topic ARN or name
     * @param subject subject of the message (ignored by most of the protocols)
     * @param message the message
     * @return the is of the message published
     */
    String publishMessageToTopic(String topicArn, String subject, String message);

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param platform
     * @param principal (ADM: clientId - APNS: sslCertificate - GCM: null)
     * @param credential (ADM: clientSecret - APNS: privateKey - GCM: apiKey)
     * @return ARN of the platform
     */
    String createPlatformApplication(String name, PLATFORM_TYPE platform, String principal, String credential);

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param platformType type of the platform
     * @param principal user's principal
     * @param credential user's credentials
     * @return ARN of the platform
     */
    @Deprecated
    String createPlatformApplication(String name, String platformType, String principal, String credential);

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param privateKey private key
     * @param sslCertificate SSL certificate
     * @param sandbox whether the application should be a iOS sandbox application
     * @return ARN of the platform
     */
    @Deprecated
    default String createIosApplication(String name, String privateKey, String sslCertificate, boolean sandbox) {
        return createPlatformApplication(name, sandbox ? PLATFORM_TYPE_IOS_SANDBOX : PLATFORM_TYPE_IOS, sslCertificate, privateKey);
    }

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param apiKey API key
     * @return ARN of the platform
     */
    @Deprecated
    default String createAndroidApplication(String name, String apiKey) {
        return createPlatformApplication(name, PLATFORM_TYPE_ANDROID, null, apiKey);
    }

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param clientId client ID
     * @param clientSecret client secret
     * @return ARN of the platform
     */
    @Deprecated
    default String createAmazonApplication(String name, String clientId, String clientSecret) {
        return createPlatformApplication(name, PLATFORM_TYPE_AMAZON, clientId, clientSecret);
    }

    /**
     * Register new device depending on patform
     * deprecated: use PUSH_PLATFORM instead of platform string
     * @param platform
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    default String registerDevice(PLATFORM_TYPE platform, String deviceToken, String customUserData) {
        return createPlatformEndpoint(getPushPlatformArn(platform), deviceToken, customUserData);
    }

    /**
     * Register new device depending on patform
     * @param platform
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    default String registerDevice(PLATFORM_TYPE platform, String deviceToken) {
        return registerDevice(platform, deviceToken, "");
    }

    /**
     * Register new device depending on patform
     * deprecated: use PUSH_PLATFORM instead of platform string
     * @param platform
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerDevice(String platform, String deviceToken, String customUserData) {
        if (MOBILE_PLATFORM_ANDROID.equals(platform)) {
            return registerAndroidDevice(deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS.equals(platform)) {
            return registerIosDevice(deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS_SANDBOX.equals(platform)) {
            return registerIosSandboxDevice(deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_AMAZON.equals(platform)) {
            return registerAmazonDevice(deviceToken, customUserData);
        }
        throw new IllegalArgumentException("Platform " + platform + " is not supported");
    }

    /**
     * Register new device depending on patform
     * @param platform
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerDevice(String platform, String deviceToken) {
        return registerDevice(platform, deviceToken, "");
    }

    /**
     * Register new Android device.
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAndroidDevice(String deviceToken) {
        return registerAndroidDevice(deviceToken, "");
    }

    /**
     * Register new Android device.
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAndroidDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getAndroidApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Deprecated - If you know the platform applicationArn, use directly createPlatformEndpoint()
     * Register new Android device.
     * @param applicationArn application ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAndroidDevice(String applicationArn, String deviceToken, String customUserData) {
        return createPlatformEndpoint(applicationArn, deviceToken, customUserData);
    }

    /**
     * Register new iOS device.
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerIosDevice(String deviceToken) {
        return registerIosDevice(deviceToken, "");
    }

    /**
     * Register new iOS device.
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerIosDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getIosApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Register new iOS Sandbox device.
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerIosSandboxDevice(String deviceToken) {
        return registerIosDevice(deviceToken, "");
    }

    /**
     * Register new iOS Sandbox device.
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerIosSandboxDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getIosSandboxApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Deprecated - If you know the platform applicationArn, use directly createPlatformEndpoint()
     * Register new device.
     * @param applicationArn application ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerIosDevice(String applicationArn, String deviceToken, String customUserData) {
        return createPlatformEndpoint(applicationArn, deviceToken, customUserData);
    }

    /**
     * Register new Amazon device.
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAmazonDevice(String deviceToken) {
        return registerAmazonDevice(deviceToken, "");
    }

    /**
     * Register new Amazon device.
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAmazonDevice(String deviceToken, String customUserData) {
        return registerAmazonDevice(getAmazonApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Deprecated - If you know the platform applicationArn, use directly createPlatformEndpoint()
     * Register new Amazon device.
     * @param applicationArn application ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    @Deprecated
    default String registerAmazonDevice(String applicationArn, String deviceToken, String customUserData) {
        return createPlatformEndpoint(applicationArn, deviceToken, customUserData);
    }

    /**
     * Creates new application endpoint.
     * @param platformApplicationArn application ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     */
    String createPlatformEndpoint(String platformApplicationArn, String deviceToken, String customUserData);

    /**
     * Creates new application endpoint.
     * @param platformApplicationArn application ARN
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    default String createPlatformEndpoint(String platformApplicationArn, String deviceToken) {
        return createPlatformEndpoint(platformApplicationArn, deviceToken, null);
    }

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @param delayWhileIdle delay while idle
     * @param timeToLive time to live
     * @param dryRun dry run
     * @return message id
     */
    @Deprecated
    String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun);

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @param delayWhileIdle delay while idle
     * @param timeToLive time to live
     * @return message id
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle, int timeToLive) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, timeToLive, false);
    }

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @param delayWhileIdle delay while idle
     * @return message id
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey, boolean delayWhileIdle) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, 125);
    }

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @return message id
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map notification, String collapseKey) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, true);
    }


    /**
     * Send iOS application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param sandbox whether the application is a sandbox application
     * @return message id
     */
    @Deprecated
    String sendIosAppNotification(String endpointArn, Map notification, boolean sandbox);

    /**
     * Send iOS application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @return message id
     */
    @Deprecated
    default String sendIosAppNotification(String endpointArn, Map notification) {
        return sendIosAppNotification(endpointArn, notification, false);
    }

    /**
     * Validates Amazon device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateAmazonDevice(String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getAmazonApplicationArn(), endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates Amazon device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateAmazonDevice(String endpointArn, String deviceToken) {
        return validateAmazonDevice(endpointArn, deviceToken, "");
    }

    /**
     * Deprecated - use validateDeviceToken()
     * Validates Android device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param applicationArn application ARN
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateAndroidDevice(String applicationArn, String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(applicationArn, endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates Android device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateAndroidDevice(String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getAndroidApplicationArn(), endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates Android device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateAndroidDevice(String endpointArn, String deviceToken) {
        return validateAndroidDevice(endpointArn, deviceToken, "");
    }

    /**
     * Deprecated - use validateDeviceToken()
     * Validates iOS device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param applicationArn application ARN
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateIosDevice(String applicationArn, String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(applicationArn, endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates iOS device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateIosDevice(String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getIosApplicationArn(), endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates iOS device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateIosDevice(String endpointArn, String deviceToken) {
        return validateIosDevice(endpointArn, deviceToken, "");
    }

    /**
     * Validates iOS Sandbox device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateIosSandboxDevice(String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getIosSandboxApplicationArn(), endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates iOS Sandbox device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateIosSandboxDevice(String endpointArn, String deviceToken) {
        return validateIosSandboxDevice(endpointArn, deviceToken, "");
    }

    /**
     * Validates Platform device.
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param platform Push platform
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    default String validatePlatformDeviceToken(PLATFORM_TYPE platform, String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getPushPlatformArn(platform), endpointArn, deviceToken, customUserData);
    }

    /**
     * Validates device token
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param applicationArn application ARN
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    String validateDeviceToken(String applicationArn, String endpointArn, String deviceToken, String customUserData);

    /**
     * Validates device token depending on platform
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateDevice(String platform, String endpointArn, String deviceToken, String customUserData) {
        if (MOBILE_PLATFORM_AMAZON.equals(platform)) {
            return validateAmazonDevice(endpointArn, deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_ANDROID.equals(platform)) {
            return validateAndroidDevice(endpointArn, deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS.equals(platform)) {
            return validateIosDevice(endpointArn, deviceToken, customUserData);
        }
        if (MOBILE_PLATFORM_IOS_SANDBOX.equals(platform)) {
            return validateIosSandboxDevice(endpointArn, deviceToken, customUserData);
        }
        return null;
    }

    /**
     * Validates device token depending on platform
     *
     * This method is able to update the custom user data as well as the type of the platform.
     *
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @return endpoint ARN which can point to different platform if required
     */
    @Deprecated
    default String validateDevice(String platform, String endpointArn, String deviceToken) {
        return validateDevice(platform, endpointArn, deviceToken, "");
    }

    /**
     * Unregisters existing device.
     * @param endpointArn endpoint ARN
     */
    void unregisterDevice(String endpointArn);

    /**
     * Send a push notification to a single mobile target - platform will be included into payload
     * https://docs.aws.amazon.com/sns/latest/dg/sns-send-custom-platform-specific-payloads-mobile-devices.html
     * @param endpointArn mobile target's arn
     * @param platformType identifier of the platform the device is registered in
     * @param jsonMessage a JSON-formatted message
     * @return notificationId
     */
    String sendPushNotification(String endpointArn, PLATFORM_TYPE platformType, String jsonMessage);

    /**
     * Send SMS
     * @param phoneNumber phone neumber in international format
     * @param message message text
     * @param smsAttributes optional SMS attributes
     * @return message ID
     */
    String sendSMSMessage(String phoneNumber, String message, Map smsAttributes);

    /**
     * Send SMS
     * @param phoneNumber phone number in international format
     * @param message message text
     * @return message ID
     */
    default String sendSMSMessage(String phoneNumber, String message) {
        return sendSMSMessage(phoneNumber, message, Collections.emptyMap());
    }
}
