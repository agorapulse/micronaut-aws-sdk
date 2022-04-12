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
package com.agorapulse.micronaut.aws.apigateway.ws

import com.agorapulse.micronaut.aws.apigateway.ws.event.EventType
import com.agorapulse.micronaut.aws.apigateway.ws.event.RequestContext
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse
import com.agorapulse.remember.Remember
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Tests for LabdaEchoFunction.
 */
@Ignore
@Remember(
    value = '2022-06-01',
    description = 'Try to fix this test or remove this feature completely'
)
class LambdaEchoFunctionSpec extends Specification {

    public static final String CONNECTION_ID = 'abcdefghij'
    public static final String BODY = 'Hello'
    public static final String RESPONSE = "[$CONNECTION_ID] ${BODY}"

    @AutoCleanup ApplicationContext ctx
    @AutoCleanup EmbeddedServer server

    LambdaEchoClient client

    MessageSender sender = Mock()
    MessageSenderFactory factory = { sender }
    TestTopicPublisher publisher = Mock()

    void setup() {
        ctx = ApplicationContext.builder().build()

        ctx.registerSingleton(MessageSenderFactory, factory)
        ctx.registerSingleton(TestTopicPublisher, publisher)

        ctx.start()

        server = ctx.getBean(EmbeddedServer).start()

        client = ctx.createBean(LambdaEchoClient, server.URL)
    }

    void 'connect'() {
        given:
            WebSocketRequest request = new WebSocketRequest(
                requestContext: new RequestContext(eventType: EventType.CONNECT, connectionId: CONNECTION_ID)
            )
        when:
            WebSocketResponse response = client.lambdaEcho(request).blockingGet()
        then:
            response.statusCode == 200

            0 * sender._
    }

    void 'disconnect'() {
        given:
            WebSocketRequest request = new WebSocketRequest(
                requestContext: new RequestContext(eventType: EventType.DISCONNECT, connectionId: CONNECTION_ID)
            )
        when:
            WebSocketResponse response = client.lambdaEcho(request).blockingGet()
        then:
            response.statusCode == 200

            0 * sender._
    }

    void 'message'() {
        given:
            WebSocketRequest request = new WebSocketRequest(
                requestContext: new RequestContext(eventType: EventType.MESSAGE, connectionId: CONNECTION_ID),
                body: BODY

            )
        when:
            WebSocketResponse response = client.lambdaEcho(request).blockingGet()
        then:
            response.statusCode == 200

            1 * sender.send(CONNECTION_ID, RESPONSE)
            1 * publisher.publishMessage(CONNECTION_ID, RESPONSE)
    }

}
