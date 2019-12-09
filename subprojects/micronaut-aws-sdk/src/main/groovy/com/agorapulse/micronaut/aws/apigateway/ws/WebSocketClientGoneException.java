package com.agorapulse.micronaut.aws.apigateway.ws;

import com.amazonaws.AmazonClientException;

public class WebSocketClientGoneException extends AmazonClientException {

    private final String connectionId;

    public WebSocketClientGoneException(String connectionId, String message, Throwable t) {
        super(message, t);
        this.connectionId = connectionId;
    }

    public WebSocketClientGoneException(String connectionId, String message) {
        super(message);
        this.connectionId = connectionId;
    }

    public WebSocketClientGoneException(String connectionId, Throwable t) {
        super(t);
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }
}
