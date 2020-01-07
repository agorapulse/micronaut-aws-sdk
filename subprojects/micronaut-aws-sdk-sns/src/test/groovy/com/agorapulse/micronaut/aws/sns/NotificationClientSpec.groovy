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
package com.agorapulse.micronaut.aws.sns

import groovy.json.JsonOutput
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
    private static final String POGO_AS_JSON = JsonOutput.toJson(POGO)
    private static final String MESSAGE_ID = '1234567890'

    SimpleNotificationService defaultService = Mock(SimpleNotificationService) {
        getDefaultTopicNameOrArn() >> DEFAULT_TOPIC
    }

    SimpleNotificationService testService = Mock(SimpleNotificationService) {
        getDefaultTopicNameOrArn() >> DEFAULT_TOPIC
    }

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.build().build()

        context.registerSingleton(SimpleNotificationService, defaultService, Qualifiers.byName('default'))
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

            1 * defaultService.publishMessageToTopic(DefaultClient.OTHER_TOPIC, null, POGO_AS_JSON) >> MESSAGE_ID
    }

    void 'can publish to default topic'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, null, POGO_AS_JSON) >> MESSAGE_ID
    }

    void 'can publish to default topic with subject'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(SUBJECT, POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, SUBJECT, POGO_AS_JSON) >> MESSAGE_ID
    }

    void 'cen publish string message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(MESSAGE)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, null, MESSAGE) >> MESSAGE_ID
    }

    void 'can publish string to default topic with subject'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String messageId = client.publishMessage(SUBJECT, MESSAGE)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(DEFAULT_TOPIC, SUBJECT, MESSAGE) >> MESSAGE_ID
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

            1 * testService.publishMessageToTopic(DEFAULT_TOPIC, null, POGO_AS_JSON) >> MESSAGE_ID
    }

    void 'can publish with different topic'() {
        given:
            StreamClient client = context.getBean(StreamClient)
        when:
            String messageId = client.publishMessage(POGO)
        then:
            messageId == MESSAGE_ID

            1 * defaultService.publishMessageToTopic(StreamClient.SOME_STREAM, null, POGO_AS_JSON) >> MESSAGE_ID
    }
}

