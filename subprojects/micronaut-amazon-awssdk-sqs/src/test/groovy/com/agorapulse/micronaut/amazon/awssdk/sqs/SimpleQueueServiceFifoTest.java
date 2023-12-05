/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::header[]
@MicronautTest                                                                          // <1>
@Property(name = "aws.sqs.queue", value = SimpleQueueServiceFifoTest.TEST_QUEUE_FIFO)            // <2>
public class SimpleQueueServiceFifoTest {

    // end::header[]
    public static final String TEST_QUEUE_FIFO = "TestQueueJava.fifo";

    private static final String DATA = "Hello World";
    private static final Map<String, String> DATA_BATCH = new HashMap<String, String>() {
        {
            put("1", "Hello World");
            put("2", "It's sunny today");
            put("3", "I'm happy");
        }
    };

    private static final Map<String, String> GROUP_ID_BATCH = new HashMap<String, String>() {
        {
            put("1", "1");
            put("2", "2");
            put("3", "3");
        }
    };

    // tag::setup[]
    @Inject SimpleQueueService service;                                                 // <3>
    // end::setup[]

    @Test
    public void testBatchMessagesWithGroupIdQueue() {
        // tag::new-queue[]
        QueueConfiguration configuration = new QueueConfiguration().withQueue(TEST_QUEUE_FIFO).withFifo(true).withContentBasedDeduplication(true);
        String queueUrl = service.createQueue(configuration);

        assertTrue(service.listQueueUrls().contains(queueUrl));                         // <2>
        // end::new-queue[]

        assertNotNull(queueUrl);

        // tag::describe-queue[]
        Map<QueueAttributeName, String> queueAttributes = service
            .getQueueAttributes(TEST_QUEUE_FIFO);                                            // <1>

        assertEquals("0", queueAttributes
            .get(QueueAttributeName.DELAY_SECONDS));                                    // <2>
        // end::describe-queue[]

        // tag::messages[]
        List<String> msgsIds = service.sendMessages(DATA_BATCH, 0, GROUP_ID_BATCH);                                       // <1>

        assertNotNull(msgsIds);
        assertEquals(3, msgsIds.size());

        List<Message> messages = service.receiveMessages(3);
        assertEquals(3, messages.size()); // <2>
        Message first = messages.get(0);
        Message second = messages.get(1);
        Message third = messages.get(2);

        assertEquals(DATA_BATCH.get("1"), first.body());                                               // <3>
        assertEquals(msgsIds.get(0), first.messageId());
        service.deleteMessage(first.receiptHandle());

        assertEquals(DATA_BATCH.get("2"), second.body());                                               // <3>
        assertEquals(msgsIds.get(1), second.messageId());
        service.deleteMessage(second.receiptHandle());

        assertEquals(DATA_BATCH.get("3"), third.body());                                               // <3>
        assertEquals(msgsIds.get(2), third.messageId());
        service.deleteMessage(third.receiptHandle());
        // end::messages[]

        List<Message> nextMessages = service.receiveMessages();

        assertEquals(0, nextMessages.size());

        // tag::delete-queue[]
        service.deleteQueue(TEST_QUEUE_FIFO);                                                // <1>

        assertFalse(service.listQueueUrls().contains(queueUrl));                        // <2>
        // end::delete-queue[]
    }
}
