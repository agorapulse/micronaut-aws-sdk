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
package com.agorapulse.micronaut.aws.apigateway.ws;

import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.function.FunctionBean;

import java.util.function.Function;

@FunctionBean("lambda-echo-java")
public class LambdaEchoJava implements Function<WebSocketRequest, WebSocketResponse> {

    private final MessageSenderFactory factory;                                         // <1>
    private final TestTopicPublisher publisher;                                         // <2>

    public LambdaEchoJava(MessageSenderFactory factory, TestTopicPublisher publisher) {
        this.factory = factory;
        this.publisher = publisher;
    }

    @Override
    public WebSocketResponse apply(WebSocketRequest event) {                            // <3>
        MessageSender sender = factory.create(event.getRequestContext());               // <4>
        String connectionId = event.getRequestContext().getConnectionId();              // <5>

        switch (event.getRequestContext().getEventType()) {
            case CONNECT:                                                               // <6>
                // do nothing
                break;
            case MESSAGE:                                                               // <7>
                String message = "[" + connectionId + "] " + event.getBody();
                sender.send(connectionId, message);
                publisher.publishMessage(connectionId, message);
                break;
            case DISCONNECT:                                                            // <8>
                // do nothing
                break;
        }

        return WebSocketResponse.OK;                                                    // <9>
    }

}
