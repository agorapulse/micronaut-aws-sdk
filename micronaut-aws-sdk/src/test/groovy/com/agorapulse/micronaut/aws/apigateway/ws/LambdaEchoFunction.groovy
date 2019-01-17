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
