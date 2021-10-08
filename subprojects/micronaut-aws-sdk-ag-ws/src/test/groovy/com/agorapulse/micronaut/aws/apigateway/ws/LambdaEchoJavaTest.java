/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.EventType;
import com.agorapulse.micronaut.aws.apigateway.ws.event.RequestContext;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@Disabled
public class LambdaEchoJavaTest {

    private static final String CONNECTION_ID = "abcdefghij";
    private static final String BODY = "Hello";
    private static final String RESPONSE = "[abcdefghij] Hello";

    private ApplicationContext ctx;
    private EmbeddedServer server;

    private LambdaEchoJavaClient client;

    private MessageSender sender;
    private MessageSenderFactory factory;
    private TestTopicPublisher publisher;

    @BeforeEach
    public void setup() {
        sender = mock(MessageSender.class);
        publisher = mock(TestTopicPublisher.class);

        factory = url -> sender;

        ctx = ApplicationContext.builder().build();

        ctx.registerSingleton(MessageSenderFactory.class, factory);
        ctx.registerSingleton(TestTopicPublisher.class, publisher);

        ctx.start();

        server = ctx.getBean(EmbeddedServer.class).start();

        client = ctx.createBean(LambdaEchoJavaClient.class, server.getURL());
    }

    @AfterEach
    public void tearDown() {
        server.close();
        ctx.close();
    }

    @Test
    public void testConnect() {
        WebSocketRequest request = new WebSocketRequest()
            .withRequestContext(
                new RequestContext().withEventType(EventType.CONNECT).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        Assertions.assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, never()).send(CONNECTION_ID, RESPONSE);
    }

    @Test
    public void testDisconnect() {
        WebSocketRequest request = new WebSocketRequest()
            .withRequestContext(
                new RequestContext().withEventType(EventType.DISCONNECT).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        Assertions.assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, never()).send(CONNECTION_ID, RESPONSE);
    }

    @Test
    public void testMessage() {
        WebSocketRequest request = new WebSocketRequest()
            .withBody(BODY)
            .withRequestContext(
                new RequestContext().withEventType(EventType.MESSAGE).withConnectionId(CONNECTION_ID)
            );

        WebSocketResponse response = client.lambdaEcho(request).blockingGet();

        Assertions.assertEquals(Integer.valueOf(200), response.getStatusCode());

        verify(sender, times(1)).send(CONNECTION_ID, RESPONSE);
        verify(publisher, times(1)).publishMessage(CONNECTION_ID, RESPONSE);
    }

}
