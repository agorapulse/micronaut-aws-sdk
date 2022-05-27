/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.Topic;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Service to simplify interaction with Amazon SNS.
 */
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

    enum PlatformType {
        ADM,
        APNS,
        APNS_SANDBOX,
        GCM
    }

    /**
     * @param platformType platform type
     * @return the default Application ARN for given platform
     */
    String getPlatformApplicationArn(PlatformType platformType);

    /**
     * @return the default Application ARN for Amazon devices integration.
     * @deprecated use {@link #getPlatformApplicationArn(PlatformType)}
     */
    @Deprecated
    String getAmazonApplicationArn();

    /**
     * @return the default Application ARN for Android devices integration.
     * @deprecated use {@link #getPlatformApplicationArn(PlatformType)}
     */
    @Deprecated
    String getAndroidApplicationArn();

    /**
     * @return the default Application ARN for iOS devices integration.
     * @deprecated use {@link #getPlatformApplicationArn(PlatformType)}
     */
    @Deprecated
    String getIosApplicationArn();

    /**
     * @return the default Application ARN for iOS sandbox devices integration.
     * @deprecated use {@link #getPlatformApplicationArn(PlatformType)}
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
    Publisher<Topic> listTopics();

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
     * @param protocol see {@link software.amazon.awssdk.services.sns.model.SubscribeRequest}
     * @param endpoint see {@link software.amazon.awssdk.services.sns.model.SubscribeRequest}
     * @return subscription ARN
     * @see #subscribeTopicWithApplication(String, String)
     * @see #subscribeTopicWithEndpoint(String, String)
     * @see #subscribeTopicWithEndpoint(String, URL)
     * @see #subscribeTopicWithEmail(String, String)
     * @see #subscribeTopicWithJsonEmail(String, String)
     * @see #subscribeTopicWithQueue(String, String)
     * @see #subscribeTopicWithSMS(String, String)
     * @see #subscribeTopicWithFunction(String, String)
     * @see software.amazon.awssdk.services.sns.model.SubscribeRequest
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
    default String publishMessageToTopic(String topicArn, String subject, String message) {
        return publishMessageToTopic(topicArn, subject, message, Collections.emptyMap());
    }

    /**
     * Publishes a message into the topic.
     * @param topicArn topic ARN or name
     * @param subject subject of the message (ignored by most of the protocols)
     * @param message the message
     * @param attributes the message attributes
     * @return the is of the message published
     */
    String publishMessageToTopic(String topicArn, String subject, String message, Map<String, String> attributes);

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param platformType
     * @param principal (ADM: clientId - APNS: sslCertificate - GCM: null)
     * @param credential (ADM: clientSecret - APNS: privateKey - GCM: apiKey)
     * @return ARN of the platform
     */
    String createPlatformApplication(String name, PlatformType platformType, String principal, String credential);

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param platformType type of the platform
     * @param principal user's principal
     * @param credential user's credentials
     * @return ARN of the platform
     */
    @Deprecated
    default String createPlatformApplication(String name, String platformType, String principal, String credential) {
        return createPlatformApplication(name, PlatformType.valueOf(platformType), principal, credential);
    }

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param privateKey private key
     * @param sslCertificate SSL certificate
     * @param sandbox whether the application should be a iOS sandbox application
     * @return ARN of the platform
     * @deprecated use {@link #createPlatformApplication(String, PlatformType, String, String)}
     */
    @Deprecated
    default String createIosApplication(String name, String privateKey, String sslCertificate, boolean sandbox) {
        return createPlatformApplication(name, sandbox ? PlatformType.APNS_SANDBOX : PlatformType.APNS, sslCertificate, privateKey);
    }

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param apiKey API key
     * @return ARN of the platform
     * @deprecated use {@link #createPlatformEndpoint(String, String, String) instead}
     */
    @Deprecated
    default String createAndroidApplication(String name, String apiKey) {
        return createPlatformApplication(name, PlatformType.GCM, null, apiKey);
    }

    /**
     * Creates new platform application.
     * @param name name of the application
     * @param clientId client ID
     * @param clientSecret client secret
     * @return ARN of the platform
     * @deprecated use {@link #createPlatformEndpoint(String, String, String) instead}
     */
    @Deprecated
    default String createAmazonApplication(String name, String clientId, String clientSecret) {
        return createPlatformApplication(name, PlatformType.ADM, clientId, clientSecret);
    }

    /**
     * Register new device depending on patform
     * @param platformType platform type
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     * @deprecated use {@link PlatformType} instead of platform string
     */
    default String registerDevice(PlatformType platformType, String deviceToken, String customUserData) {
        return createPlatformEndpoint(getPlatformApplicationArn(platformType), deviceToken, customUserData);
    }

    /**
     * Register new device depending on patform
     * @param platformType
     * @param deviceToken device token
     * @return ARN of the endpoint
     */
    default String registerDevice(PlatformType platformType, String deviceToken) {
        return registerDevice(platformType, deviceToken, "");
    }

    /**
     * Register new device depending on patform
     * @param platform
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     * @deprecated use {@link PlatformType} instead of platform string
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
     */
    @Deprecated
    default String registerIosDevice(String deviceToken, String customUserData) {
        return createPlatformEndpoint(getIosApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Register new iOS Sandbox device.
     * @param deviceToken device token
     * @return ARN of the endpoint
     * @deprecated use {@link #registerDevice(PlatformType, String)}
     */
    @Deprecated
    default String registerIosSandboxDevice(String deviceToken) {
        return registerIosSandboxDevice(deviceToken, "");
    }

    /**
     * Register new iOS Sandbox device.
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     * @deprecated use {@link #registerDevice(PlatformType, String)}
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
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
     * @deprecated use {@link #registerDevice(PlatformType, String)}
     */
    @Deprecated
    default String registerAmazonDevice(String deviceToken, String customUserData) {
        return registerAmazonDevice(getAmazonApplicationArn(), deviceToken, customUserData);
    }

    /**
     * Register new Amazon device.
     * @param applicationArn application ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return ARN of the endpoint
     * @deprecated use {@link #createPlatformEndpoint(String, String)}
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
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey, boolean delayWhileIdle, int timeToLive, boolean dryRun);

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @param delayWhileIdle delay while idle
     * @param timeToLive time to live
     * @return message id
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey, boolean delayWhileIdle, int timeToLive) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, timeToLive, false);
    }

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @param delayWhileIdle delay while idle
     * @return message id
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey, boolean delayWhileIdle) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, delayWhileIdle, 125);
    }

    /**
     * Send Android application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param collapseKey collapse key
     * @return message id
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    default String sendAndroidAppNotification(String endpointArn, Map<String, Object> notification, String collapseKey) {
        return sendAndroidAppNotification(endpointArn, notification, collapseKey, true);
    }


    /**
     * Send iOS application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @param sandbox whether the application is a sandbox application
     * @return message id
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    String sendIosAppNotification(String endpointArn, Map<String, Object> notification, boolean sandbox);

    /**
     * Send iOS application notification.
     * @param endpointArn endpoint ARN
     * @param notification notification payload
     * @return message id
     * @deprecated use {@link #sendNotification(String, PlatformType, String)}
     */
    @Deprecated
    default String sendIosAppNotification(String endpointArn, Map<String, Object> notification) {
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @param platformType platform type
     * @param endpointArn endpoint ARN
     * @param deviceToken device token
     * @param customUserData custom user data
     * @return endpoint ARN which can point to different platform if required
     */
    default String validateDeviceToken(PlatformType platformType, String endpointArn, String deviceToken, String customUserData) {
        return validateDeviceToken(getPlatformApplicationArn(platformType), endpointArn, deviceToken, customUserData);
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
     * @deprecated use {@link #validateDeviceToken(String, String, String, String)}
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
    String sendNotification(String endpointArn, PlatformType platformType, String jsonMessage);

    /**
     * Send SMS
     * @param phoneNumber phone neumber in international format
     * @param message message text
     * @param smsAttributes optional SMS attributes
     * @return message ID
     */
    String sendSMSMessage(String phoneNumber, String message, Map<String, MessageAttributeValue> smsAttributes);

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
