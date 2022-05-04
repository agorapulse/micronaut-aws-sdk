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
package com.agorapulse.micronaut.amazon.awssdk.sns

import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import spock.lang.PendingFeature
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject

/**
 * Tests for simple notification service.
 */
@Stepwise
// tag::testcontainers-header[]
@MicronautTest
@Property(name = 'aws.sns.topic', value = TEST_TOPIC)
@Property(name = 'aws.sns.ios.arn', value = IOS_APP_ARN)
@Property(name = 'aws.sns.ios-sandbox.arn', value = IOS_SANDBOX_APP_ARN)
@Property(name = 'aws.sns.android.arn', value = ANDROID_APP_ARN)
@Property(name = 'aws.sns.amazon.arn', value = AMAZON_APP_ARN)
class SimpleNotificationServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final String TEST_TOPIC = 'TestTopic'
    private static final String IOS_APP_ARN = 'arn:aws:sns:us-east-1:000000000000:app/APNS/IOS-APP'
    private static final String IOS_SANDBOX_APP_ARN = 'arn:aws:sns:us-east-1:000000000000:app/APNS_SANDBOX/IOS-APP'
    private static final String ANDROID_APP_ARN = 'arn:aws:sns:us-east-1:000000000000:app/GCM/ANDROID-APP'
    private static final String AMAZON_APP_ARN = 'arn:aws:sns:us-east-1:000000000000:app/ADM/AMAZON-APP'

    @Inject SimpleNotificationService service
    @Inject SimpleNotificationServiceConfiguration configuration
    // end::testcontainers-fields[]

    @Shared String androidEndpointArn
    @Shared String amazonEndpointArn
    @Shared String iosEndpointArn
    @Shared String iosSandboxEndpointArn
    @Shared String subscriptionArn
    @Shared String topicArn

    // tag::testcontainers-setup[]
    void setup() {
        service.createIosApplication('IOS-APP', 'API-KEY', 'fake-cert', false)
        service.createIosApplication('IOS-APP', 'API-KEY', 'fake-cert', true)
        service.createAndroidApplication('ANDROID-APP', 'API-KEY')
        service.createAmazonApplication('AMAZON-APP', 'API-KEY', 'API-SECRET')
    }

    void 'new topic'() {
        when:
            String testTopic = 'TOPIC'
            String created = service.createTopic(testTopic)
            topicArn = testTopic
        then:
            created.endsWith(testTopic)
    }

    void 'subscribe to the topic'() {
        when:
            subscriptionArn = service.subscribeTopicWithEmail(topicArn, 'vlad@agorapulse.com')
        then:
            subscriptionArn
    }

    void 'subscribe to the json email'() {
        when:
            subscriptionArn = service.subscribeTopicWithJsonEmail(topicArn, 'vlad@agorapulse.com')
        then:
            subscriptionArn
    }

    void 'register android device'() {
        when:
            androidEndpointArn = service.registerDevice(SimpleNotificationService.MOBILE_PLATFORM_ANDROID, 'ANDROID-TOKEN')
        then:
            androidEndpointArn
        expect:
            service.registerAndroidDevice('ANOTHER-ANDROID-TOKEN')
    }

    void 'register amazon device'() {
        when:
            amazonEndpointArn = service.registerDevice(SimpleNotificationService.MOBILE_PLATFORM_AMAZON, 'AMAZON-TOKEN')
        then:
            amazonEndpointArn
        expect:
            service.registerAmazonDevice('ANOTHER-AMAZON-TOKEN')
    }

    void 'register iOS device'() {
        when:
            iosEndpointArn = service.registerDevice(SimpleNotificationService.MOBILE_PLATFORM_IOS, 'IOS-TOKEN')
        then:
            iosEndpointArn
        expect:
            service.registerIosDevice('ANOTHER-IOS-TOKEN')
    }

    void 'register iOS sandbox device'() {
        when:
            iosSandboxEndpointArn = service.registerDevice(SimpleNotificationService.MOBILE_PLATFORM_IOS_SANDBOX, 'IOS-SANDBOX-TOKEN')
        then:
            iosSandboxEndpointArn
        expect:
            service.registerIosSandboxDevice('ANOTHER-IOS-SANDBOX-TOKEN')
    }

    @PendingFeature(reason = 'Needs to be tested with real SNS')
    void 'register iOS sandbox device with the same token'() {
        expect:
            iosSandboxEndpointArn == service.createPlatformEndpoint(configuration.iosSandbox.arn, 'IOS-SANDBOX-TOKEN')
    }

    void 'register unknown device'() {
        when:
            service.registerDevice('foo', 'FOO-SANDBOX-TOKEN')
        then:
            thrown(IllegalArgumentException)
    }

    void 'publish message'() {
        when:
            String messageId = service.publishMessageToTopic(topicArn, 'SUBJECT', 'MESSAGE')
        then:
            messageId
    }

    void 'validate android device'() {
        expect:
            androidEndpointArn == service.validateAndroidDevice(androidEndpointArn, 'ANDROID-TOKEN')
            androidEndpointArn == service.validateDevice(SimpleNotificationService.MOBILE_PLATFORM_ANDROID, androidEndpointArn, 'ANDROID-TOKEN-NEW')
    }

    void 'validate amazon device'() {
        expect:
            amazonEndpointArn == service.validateAmazonDevice(amazonEndpointArn, 'AMAZON-TOKEN')
            amazonEndpointArn == service.validateDevice(SimpleNotificationService.MOBILE_PLATFORM_AMAZON, amazonEndpointArn, 'AMAZON-TOKEN')
    }

    void 'validate ios device'() {
        expect:
            iosEndpointArn == service.validateIosDevice(iosEndpointArn, 'IOS-TOKEN')
            iosEndpointArn == service.validateDevice(SimpleNotificationService.MOBILE_PLATFORM_IOS, iosEndpointArn, 'IOS-TOKEN')
    }

    void 'validate ios sandbox device'() {
        expect:
            iosSandboxEndpointArn == service.validateIosSandboxDevice(iosSandboxEndpointArn, 'IOS-SANDBOX-TOKEN')
            iosSandboxEndpointArn == service.validateDevice(SimpleNotificationService.MOBILE_PLATFORM_IOS_SANDBOX, iosSandboxEndpointArn, 'IOS-SANDBOX-TOKEN')
    }

    void 'validate unknown platform'() {
        expect:
            !service.validateDevice('foo', 'arn', 'baz')
    }

    void 'publish direct'() {
        expect:
            service.sendAndroidAppNotification(androidEndpointArn, [message: 'Hello'], 'key')
            service.sendIosAppNotification(iosEndpointArn, [message: 'Hello'])
    }

    void 'unregister device'() {
        when:
            service.unregisterDevice(androidEndpointArn)
        then:
            noExceptionThrown()
    }

    @PendingFeature(reason = 'Needs to be tested with real SNS')
    void 'validate unregistered device'() {
        expect:
            service.validateAndroidDevice(androidEndpointArn, 'ANDROID-TOKEN')
    }

    void 'subscribe to the application'() {
        when:
            subscriptionArn = service.subscribeTopicWithApplication(topicArn, 'fake-app-arn')
        then:
            subscriptionArn
    }

    void 'subscribe to the lambda'() {
        when:
            subscriptionArn = service.subscribeTopicWithFunction(topicArn, 'fake-fun-arn')
        then:
            subscriptionArn
    }

    void 'subscribe to the queue'() {
        when:
            subscriptionArn = service.subscribeTopicWithQueue(topicArn, 'fake-queue-arn')
        then:
            subscriptionArn
    }

    void 'subscribe to the sms'() {
        when:
            subscriptionArn = service.subscribeTopicWithSMS(topicArn, '+420555666777')
        then:
            subscriptionArn
    }

    void 'subscribe to the topic with http'() {
        when:
            subscriptionArn = service.subscribeTopicWithEndpoint(topicArn, 'http://www.example.com')
        then:
            subscriptionArn
    }

    void 'subscribe to the topic with https'() {
        when:
            subscriptionArn = service.subscribeTopicWithEndpoint(topicArn, 'https://www.example.com')
        then:
            subscriptionArn
    }

    void 'can only subscribe with http or https'() {
        when:
            service.subscribeTopicWithEndpoint(topicArn, 'file:///tmp/foo.bar')
        then:
            thrown(IllegalArgumentException)
    }

    void 'unsubscribe from the topic'() {
        when:
            service.unsubscribeTopic(subscriptionArn)
        then:
            noExceptionThrown()
    }

    void 'delete topic'() {
        when:
            service.deleteTopic(topicArn)
        then:
            noExceptionThrown()
    }

    void 'send sms'() {
        expect:
            service.sendSMSMessage('+420555666777', 'Hello')
    }

}
