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
package com.agorapulse.micronaut.amazon.awssdk.sns

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import spock.lang.AutoCleanup
import spock.lang.PendingFeature
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

/**
 * Tests for simple notification service.
 */
@Stepwise
// tag::testcontainers-header[]
@Testcontainers                                                                         // <1>
class SimpleNotificationServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final String TEST_TOPIC = 'TestTopic'

    // tag::testcontainers-fields[]
    @Shared LocalStackContainer localstack = new LocalStackContainer('0.8.10')          // <2>
        .withServices(LocalStackContainer.Service.SNS)

    @AutoCleanup ApplicationContext context                                             // <3>

    SimpleNotificationService service
    SimpleNotificationServiceConfiguration configuration
    // end::testcontainers-fields[]

    @Shared String androidEndpointArn
    @Shared String amazonEndpointArn
    @Shared String iosEndpointArn
    @Shared String iosSandboxEndpointArn
    @Shared String subscriptionArn
    @Shared String topicArn

    // tag::testcontainers-setup[]
    void setup() {
        SnsClient sns = SnsClient                                                 // <4>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SNS))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.defaultAccessKey, localstack.defaultSecretKey
            )))
            .region(Region.of(localstack.defaultRegion))
            .build()

        SimpleNotificationServiceConfiguration mockConfiguration = Mock(SimpleNotificationServiceConfiguration) {
            getTopic() >> TEST_TOPIC
        }

        DefaultSimpleNotificationService configurer = new DefaultSimpleNotificationService(sns, mockConfiguration, new ObjectMapper())

        context = ApplicationContext.build(
            'aws.sns.topic': TEST_TOPIC,
            'aws.sns.ios.arn': configurer.createIosApplication('IOS-APP', 'API-KEY', 'fake-cert', false),
            'aws.sns.ios-sandbox.arn': configurer.createIosApplication('IOS-APP', 'API-KEY', 'fake-cert', true),
            'aws.sns.android.arn': configurer.createAndroidApplication('ANDROID-APP', 'API-KEY'),
            'aws.sns.amazon.arn': configurer.createAmazonApplication('AMAZON-APP', 'API-KEY', 'API-SECRET')
        ).build()         // <5>

        context.registerSingleton(SnsClient, sns)
        context.start()

        service = context.getBean(SimpleNotificationService)                            // <6>
        configuration = context.getBean(SimpleNotificationServiceConfiguration)
    }
    // end::testcontainers-setup[]

    void 'new topic'() {
        when:
            topicArn = service.createTopic('TOPIC')
        then:
            topicArn
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
            service.sendIosAppNotification(iosEndpointArn, [message: "Hello"])
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
