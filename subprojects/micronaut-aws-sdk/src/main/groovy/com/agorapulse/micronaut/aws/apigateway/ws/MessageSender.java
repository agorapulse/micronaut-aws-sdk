package com.agorapulse.micronaut.aws.apigateway.ws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Message sender sends messages to WebSocket's clients
 */
public interface MessageSender {

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    default void send(String connectionId, String payload) {
        send(connectionId, new ByteArrayInputStream(payload.getBytes()));
    }

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    void send(String connectionId, InputStream payload);

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    void send(String connectionId, Object payload);

}
