package com.agorapulse.micronaut.aws.apigateway.ws.event;

import java.util.Objects;

/**
 * WebSocket response which always needs to be sent back from the function handling any of API Gateway WebSocket
 * proxy events.
 */
public class WebSocketResponse {

    public static final WebSocketResponse OK = new WebSocketResponse(200);
    public static final WebSocketResponse ERROR = new WebSocketResponse(500);

    private final Integer statusCode;

    public WebSocketResponse(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketResponse that = (WebSocketResponse) o;
        return Objects.equals(statusCode, that.statusCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode);
    }

    @Override
    public String toString() {
        return "WebSocketResponse{" +
            "statusCode=" + statusCode +
            '}';
    }
}
