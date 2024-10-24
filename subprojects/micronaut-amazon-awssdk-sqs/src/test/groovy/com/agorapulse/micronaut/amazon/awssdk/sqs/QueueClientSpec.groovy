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
package com.agorapulse.micronaut.amazon.awssdk.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests or queue client.
 */
class QueueClientSpec extends Specification {

    private static final String DEFAULT_QUEUE_NAME = 'DefaultQueue'
    private static final String MESSAGE = 'MESSAGE'
    private static final Pogo POGO = new Pogo(foo: 'bar')
    private static final String GROUP = 'SomeGroup'
    private static final int DELAY = 10
    private static final String ID = '12345'

    SimpleQueueService defaultService = Mock(SimpleQueueService) {
        getDefaultQueueName() >> DEFAULT_QUEUE_NAME
    }

    SimpleQueueService testService = Mock(SimpleQueueService) {
        getDefaultQueueName() >> DEFAULT_QUEUE_NAME
    }

    @AutoCleanup ApplicationContext context

    String marshalledPogo

    void setup() {
        context = ApplicationContext.builder().build()

        context.registerSingleton(SimpleQueueService, defaultService)
        context.registerSingleton(SimpleQueueService, testService, Qualifiers.byName('test'))

        context.start()

        marshalledPogo = context.getBean(ObjectMapper).writeValueAsString(POGO)
    }

    void 'can send message to other than default queue potentionally altering the group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessageToQueue(MESSAGE)
        then:
            id == ID

            1 * defaultService.sendMessage(DefaultClient.OTHER_QUEUE, MESSAGE, 0, GROUP) >> ID
    }

    void 'can send a single message and return id'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, marshalledPogo, 0, null) >> ID
    }

    void 'can send a single message and return id with specified configuration name'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * testService.sendMessage(DEFAULT_QUEUE_NAME, marshalledPogo, 0, null) >> ID
    }

    void 'can send a single byte array message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE.bytes)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, null) >> ID
    }

    void 'can send a single string message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, null) >> ID
    }

    void 'can send a single string message with delay'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, DELAY)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, DELAY, null) >> ID
    }

    void 'can send a single string message with group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, GROUP)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, GROUP) >> ID
    }

    void 'can send a single string message with delay and group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, DELAY, GROUP)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, DELAY, GROUP) >> ID
    }

    void 'can send multiple messages when publisher is a parameter'() {
        given:
            List<String> ids = [ID + 1, ID + 2, ID + 3]
            DefaultClient client = context.getBean(DefaultClient)

        when:
            Publisher<String> messages = client.sendMessages(Flux.just(POGO, POGO, POGO))

        then:
            Flux.from(messages).collectList().block() == ids

            1 * defaultService.sendMessages(DEFAULT_QUEUE_NAME, _ as Publisher, 0, null) >> Flux.just(ID + 1, ID + 2, ID + 3)
    }

    void 'can send multiple string messages and return void'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.sendStringMessages(Flux.just(MESSAGE, MESSAGE, MESSAGE))
        then:
            1 * defaultService.sendMessages(DEFAULT_QUEUE_NAME, _ as Publisher, 0, null) >> Flux.just(ID + 1, ID + 2, ID + 3)
    }

    void 'needs to follow the method convention rules'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            client.doWhatever(POGO, POGO, POGO, POGO)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'can delete message by id'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.deleteMessage(ID)
        then:
            1 * defaultService.deleteMessage(DEFAULT_QUEUE_NAME, ID)
    }

    void 'can send message with specified queue name'() {
        given:
            SomeClient client = context.getBean(SomeClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * defaultService.sendMessage(SomeClient.SOME_QUEUE, marshalledPogo, DELAY, null) >> ID
    }

}
