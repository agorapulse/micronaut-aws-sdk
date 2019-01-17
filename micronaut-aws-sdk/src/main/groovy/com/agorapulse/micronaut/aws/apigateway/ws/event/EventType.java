package com.agorapulse.micronaut.aws.apigateway.ws.event;

/**
 * Type of the WebSocket event.
 */
public enum EventType {
    /**
     * Type of event sent when the client is connected.
     */
    CONNECT,

    /**
     * Type of event sent when the incoming message arrives.
     */
    MESSAGE,

    /**
     * Type of event sent when the client disconnects.
     *
     * There is no guarantee this event is triggered.
     */
    DISCONNECT
}
