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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import javax.inject.Inject;
import java.util.*;

import static org.junit.Assert.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@MicronautTest
@Property(name = "aws.sqs.queue", value = SimpleQueueServiceTest.TEST_QUEUE)
public class SimpleQueueServiceTest {

    public static final String TEST_QUEUE = "TestQueue";

    private static final String DATA = "Hello World";

    @Rule
    public Retry retry = new Retry(5);

    @Inject SimpleQueueService service;

    @Test
    public void testWorkingWithQueue() {
        // tag::new-queue[]
        String queueUrl = service.createQueue(TEST_QUEUE);                              // <1>

        assertTrue(service.listQueueUrls().contains(queueUrl));                         // <2>
        // end::new-queue[]

        assertNotNull(queueUrl);

        // tag::describe-queue[]
        Map<QueueAttributeName, String> queueAttributes = service
            .getQueueAttributes(TEST_QUEUE);                                            // <1>

        assertEquals("0", queueAttributes
            .get(QueueAttributeName.DELAY_SECONDS));                                    // <2>
        // end::describe-queue[]

        // tag::messages[]
        String msgId = service.sendMessage(DATA);                                       // <1>

        assertNotNull(msgId);

        List<Message> messages = service.receiveMessages();                             // <2>
        Message first = messages.get(0);

        assertEquals(DATA, first.body());                                               // <3>
        assertEquals(msgId, first.messageId());
        assertEquals(1, messages.size());

        service.deleteMessage(msgId);                                                   // <4>
        // end::messages[]

        List<Message> nextMessages = service.receiveMessages();

        assertEquals(0, nextMessages.size());

        // tag::delete-queue[]
        service.deleteQueue(TEST_QUEUE);                                                // <1>

        assertFalse(service.listQueueUrls().contains(queueUrl));                        // <2>
        // end::delete-queue[]
    }

}
