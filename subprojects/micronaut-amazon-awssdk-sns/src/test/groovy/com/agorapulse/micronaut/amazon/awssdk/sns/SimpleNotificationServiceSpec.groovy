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

import io.micronaut.context.ApplicationContext
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import spock.lang.AutoCleanup
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
    // end::testcontainers-fields[]

    @Shared String endpointArn
    @Shared String platformApplicationArn
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

        context = ApplicationContext.build('aws.sns.topic', TEST_TOPIC).build()         // <5>
        context.registerSingleton(SnsClient, sns)
        context.start()

        service = context.getBean(SimpleNotificationService)                            // <6>
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

    void 'new platform application'() {
        when:
            platformApplicationArn = service.createAndroidApplication('ANDROID-APP', 'API-KEY')
        then:
            platformApplicationArn
    }

    void 'register device'() {
        when:
            endpointArn = service.registerAndroidDevice(platformApplicationArn, 'TOKEN', 'CUSTOMER-DATA')
        then:
            endpointArn
    }

    void 'publish message'() {
        when:
            String messageId = service.publishMessageToTopic(topicArn, 'SUBJECT', 'MESSAGE')
        then:
            messageId
    }

    void 'validate device'() {
        when:
            String newEndpointArn = service.validateAndroidDevice(platformApplicationArn, endpointArn, 'OTHER-TOKEN', 'OTHER-CUSTOMER-DATA')
        then:
            endpointArn == newEndpointArn
    }

    void 'publish direct'() {
        when:
            String messageId = service.sendAndroidAppNotification(endpointArn, [message: 'Hello'], 'key')
        then:
            messageId
    }

    void 'unregister device'() {
        when:
            service.unregisterDevice(endpointArn)
        then:
            noExceptionThrown()
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

}
