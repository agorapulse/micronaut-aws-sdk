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
package com.agorapulse.micronaut.aws.apigateway.ws

import com.agorapulse.micronaut.aws.apigateway.ws.event.EventType
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse
import groovy.transform.Field

import javax.inject.Inject

@Inject @Field MessageSenderFactory factory                                             // <1>
@Inject @Field TestTopicPublisher publisher                                             // <2>

WebSocketResponse lambdaEcho(WebSocketRequest event) {                                  // <3>
    MessageSender sender = factory.create(event.requestContext)                         // <4>
    String connectionId = event.requestContext.connectionId                             // <5>

    switch (event.requestContext.eventType) {
        case EventType.CONNECT:                                                         // <6>
            // do nothing
            break
        case EventType.MESSAGE:                                                         // <7>
            String message = "[$connectionId] ${event.body}"
            sender.send(connectionId, message)
            publisher.publishMessage(connectionId, message)
            break
        case EventType.DISCONNECT:                                                      // <8>
            // do nothing
            break
    }

    return WebSocketResponse.OK                                                         // <9>
}
