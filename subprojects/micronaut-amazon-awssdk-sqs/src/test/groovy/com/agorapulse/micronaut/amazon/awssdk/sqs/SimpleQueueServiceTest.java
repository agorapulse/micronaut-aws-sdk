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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import io.micronaut.context.ApplicationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.*;

import static org.junit.Assert.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

public class SimpleQueueServiceTest {

    private static final String TEST_QUEUE = "TestQueue";
    private static final String DATA = "Hello World";

    @Rule
    public Retry retry = new Retry(5);

    // tag::testcontainers-setup[]
    public ApplicationContext context;                                                  // <1>

    public SimpleQueueService service;

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer("0.8.10")           // <2>
        .withServices(SQS);

    @Before
    public void setup() {
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");                    // <3>

        AmazonSQS amazonSQS = AmazonSQSClient                                           // <4>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(SQS))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();


        Map<String, Object> properties = new HashMap<>();                               // <5>
        properties.put("aws.sqs.queue", TEST_QUEUE);


        context = ApplicationContext.build(properties).build();                         // <6>
        context.registerSingleton(AmazonSQS.class, amazonSQS);
        context.start();

        service = context.getBean(SimpleQueueService.class);
    }

    @After
    public void cleanup() {
        System.clearProperty("com.amazonaws.sdk.disableCbor");

        if (context != null) {
            context.close();                                                            // <7>
        }
    }
    // end::testcontainers-setup[]

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
