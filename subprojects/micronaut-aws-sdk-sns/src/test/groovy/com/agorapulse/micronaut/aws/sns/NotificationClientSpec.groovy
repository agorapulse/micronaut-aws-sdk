/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests for notification client.
 */
class NotificationClientSpec extends Specification {

    private static final String DEFAULT_TOPIC = 'DefaultTopic'
    private static final Pogo POGO = new Pogo(foo: 'bar')
    private static final String MESSAGE = 'Hello'
    private static final String SUBJECT = 'Subject'
    private static final String PHONE_NUMBER = '+883510000000094'
    private static final Map SMS_ATTRIBUTES = Collections.singletonMap('foo', 'bar')
    private static final Map PUBLISH_ATTRIBUTES = Collections.singletonMap('attr', 'value')
    private static final Map EMPTY_MAP = Collections.emptyMap()
    private static final String POGO_AS_JSON = new ObjectMapper().writeValueAsString(POGO)
    private static final String MESSAGE_ID = '1234567890'
    private static final String MESSAGE_GROUP_ID = 'messageGroupId1'
    private static final String MESSAGE_DEDUPLICATION_ID = 'messageDeduplicationId1'

    SimpleNotificationService defaultService = Mock(SimpleNotificationService) {
        getDefaultTopicNameOrArn() >> DEFAULT_TOPIC
    }

    SimpleNotificationService testService = Mock(SimpleNotificationService) {
        getDefaultTopicNameOrArn() >> DEFAULT_TOPIC
    }

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.builder().build()

        context.registerSingleton(SimpleNotificationService, defaultService)
        context.registerSingleton(SimpleNotificationService, testService, Qualifiers.byName('test'))

        context.start()
    }

    void 'can publish to different topic'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessageToDifferentTopic(POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DefaultClient.OTHER_TOPIC, null, POGO_AS_JSON, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish to default topic'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, null, POGO_AS_JSON, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish to default topic with subject'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(SUBJECT, POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, SUBJECT, POGO_AS_JSON, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish to default topic with subject and attributes'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(SUBJECT, POGO, PUBLISH_ATTRIBUTES)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, SUBJECT, POGO_AS_JSON, PUBLISH_ATTRIBUTES) >> MESSAGE_ID
    }

    void 'can publish string message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(MESSAGE)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, null, MESSAGE, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish string to default topic with subject'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(SUBJECT, MESSAGE)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, SUBJECT, MESSAGE, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can send SMS'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.sendSMS(PHONE_NUMBER, MESSAGE)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.sendSMSMessage(PHONE_NUMBER, MESSAGE, [:]) >> MESSAGE_ID
    }

    void 'can send SMS with additonal attributtes'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.sendSms(PHONE_NUMBER, MESSAGE, SMS_ATTRIBUTES)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.sendSMSMessage(PHONE_NUMBER, MESSAGE, SMS_ATTRIBUTES) >> MESSAGE_ID
    }

    void 'can publish with different configuration'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            String messageId = client.publishMessage(POGO)
        then:
            messageId == MESSAGE_ID

            1 * testService.publishMessageToTopic(DEFAULT_TOPIC, null, POGO_AS_JSON, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish with different topic'() {
        given:
            StreamClient client = context.getBean(StreamClient)
        when:
            String messageId = client.publishMessage(POGO)
        then:
            messageId == MESSAGE_ID
            1 * defaultService.publishMessageToTopic(StreamClient.SOME_STREAM, null, POGO_AS_JSON, EMPTY_MAP) >> MESSAGE_ID
    }

    void 'can publish pojo to fifo topic'() {
        given:
        TestFifoClient client = context.getBean(TestFifoClient)

        when:
        String messageId = client.publishFifoMessage(POGO, MESSAGE_GROUP_ID, MESSAGE_DEDUPLICATION_ID)

        then:
        1 * testService.publishRequest(TestFifoClient.TOPIC_NAME, EMPTY_MAP, _) >> { String topicArn,
                                                                                     Map<String, String> attributes,
                                                                                     PublishRequest publishRequest ->
            assert publishRequest.messageGroupId == MESSAGE_GROUP_ID
            assert publishRequest.messageDeduplicationId ==  MESSAGE_DEDUPLICATION_ID
            assert publishRequest.message == POGO_AS_JSON
            return MESSAGE_ID
        }
        messageId == MESSAGE_ID
    }

}

