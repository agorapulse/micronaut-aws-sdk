package com.agorapulse.micronaut.aws.apigateway.ws;

import com.amazonaws.AmazonClientException;

public class WebSocketClientGoneException extends AmazonClientException {

    public WebSocketClientGoneException(String message, Throwable t) {
        super(message, t);
    }

    public WebSocketClientGoneException(String message) {
        super(message);
    }

    public WebSocketClientGoneException(Throwable t) {
        super(t);
    }

}
