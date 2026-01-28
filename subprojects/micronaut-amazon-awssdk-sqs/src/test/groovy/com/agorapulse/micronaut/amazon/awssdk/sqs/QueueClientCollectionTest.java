/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;

import jakarta.inject.Inject;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
@Property(name = "aws.sqs.queue", value = QueueClientCollectionTest.TEST_QUEUE)
class QueueClientCollectionTest {

    static final String TEST_QUEUE = "TestQueueCollection";

    private static final String MESSAGE = "Hello World";

    @Inject
    SimpleQueueService service;

    @Inject
    DefaultClient client;

    private String testThreadName;

    @BeforeEach
    void setup() {
        // simulate non-blocking thread to test the blocking executor pattern
        testThreadName = Thread.currentThread().getName();
        Schedulers.registerNonBlockingThreadPredicate((Predicate<Thread>) t -> t.getName().equals(testThreadName));

        service.createQueue(TEST_QUEUE);
    }

    @AfterEach
    void cleanup() {
        Schedulers.resetNonBlockingThreadPredicate();
        service.deleteQueue(TEST_QUEUE);
    }

    @Test
    void canSendMultipleStringMessagesWhenListIsParameterAndReturnListOfIds() {
        List<String> result = client.sendStringMessages(List.of(MESSAGE, MESSAGE, MESSAGE));

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(Assertions::assertNotNull);
    }

    @Test
    void canSendMultipleStringMessagesWhenArrayIsParameterAndReturnListOfIds() {
        String[] messages = new String[]{MESSAGE, MESSAGE, MESSAGE};

        List<String> result = client.sendStringMessages(messages);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(Assertions::assertNotNull);
    }

    @Test
    void canSendMultipleMessagesWhenListIsParameterAndReturnListOfIds() {
        Pogo pogo = new Pogo();
        pogo.setFoo("bar");

        List<String> result = client.sendMessages(List.of(pogo, pogo, pogo));

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(Assertions::assertNotNull);
    }

    @Test
    void canSendMultipleMessagesWhenArrayIsParameterAndReturnListOfIds() {
        Pogo pogo = new Pogo();
        pogo.setFoo("bar");
        Pogo[] messages = new Pogo[]{pogo, pogo, pogo};

        List<String> result = client.sendMessages(messages);

        assertNotNull(result);
        assertEquals(3, result.size());
        result.forEach(Assertions::assertNotNull);
    }

    @Test
    void canSendMultipleMessagesWhenListIsParameterAndReturnVoid() {
        Pogo pogo = new Pogo();
        pogo.setFoo("bar");

        // should not throw any exception
        client.sendMessagesVoid(List.of(pogo, pogo, pogo));
    }

}
