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
package com.agorapulse.micronaut.aws.sqs;

import com.amazonaws.services.sqs.model.Message;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "aws.sqs.queue", value = SimpleQueueServiceTest.TEST_QUEUE)
public class SimpleQueueServiceTest {

    public static final String TEST_QUEUE = "TestQueueJava";
    private static final String DATA = "Hello World";

    @Inject SimpleQueueService service;

    @Test
    public void testWorkingWithQueue() {
        // tag::new-queue[]
        String queueUrl = service.createQueue(TEST_QUEUE);                              // <1>

        assertTrue(service.listQueueUrls().contains(queueUrl));                         // <2>
        // end::new-queue[]

        assertNotNull(queueUrl);

        // tag::describe-queue[]
        Map<String, String> queueAttributes = service.getQueueAttributes(TEST_QUEUE);   // <1>

        assertEquals("0", queueAttributes.get("DelaySeconds"));                         // <2>
        // end::describe-queue[]

        // tag::messages[]
        String msgId = service.sendMessage(DATA);                                       // <1>

        assertNotNull(msgId);

        List<Message> messages = service.receiveMessages();                             // <2>
        Message first = messages.get(0);

        assertEquals(DATA, first.getBody());                                            // <3>
        assertEquals(msgId, first.getMessageId());
        assertEquals(1, messages.size());

        service.deleteMessage(first.getReceiptHandle());                                // <4>
        // end::messages[]

        List<Message> nextMessages = service.receiveMessages();

        assertEquals(0, nextMessages.size());

        // tag::delete-queue[]
        service.deleteQueue(TEST_QUEUE);                                                // <1>

        assertFalse(service.listQueueUrls().contains(queueUrl));                        // <2>
        // end::delete-queue[]
    }

}
