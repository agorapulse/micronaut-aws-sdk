package com.agorapulse.micronaut.aws.apigateway.ws.event;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Event which can be used as a parameter of Lambda functions which handles <code>$connect</code> or <code>$disconnect</code> route.
 */
public class WebSocketConnectionRequest extends WebSocketRequest {

    private Map<String, String> headers;
    private Map<String, List<String>> multiValueHeaders;

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WebSocketConnectionRequest that = (WebSocketConnectionRequest) o;
        return Objects.equals(headers, that.headers) &&
            Objects.equals(multiValueHeaders, that.multiValueHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), headers, multiValueHeaders);
    }

    @Override
    public String toString() {
        return "WebSocketConnectionRequest{" +
            "headers=" + headers +
            ", multiValueHeaders=" + multiValueHeaders +
            '}';
    }
}
