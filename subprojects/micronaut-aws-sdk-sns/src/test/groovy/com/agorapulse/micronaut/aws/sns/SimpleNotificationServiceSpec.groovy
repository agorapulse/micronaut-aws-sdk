/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.aws.sns

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
class SimpleNotificationServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final String TEST_TOPIC = 'TestTopic'

    @Inject SimpleNotificationService service
    // end::testcontainers-fields[]

    @Shared String endpointArn
    @Shared String platformApplicationArn
    @Shared String subscriptionArn
    @Shared String topicArn

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

    @PendingFeature(
        reason = 'GCM moved int Firebase Console, latest Localstack fails with "Invalid parameter: Attributes Reason: Platform credentials are invalid"'
    )
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
