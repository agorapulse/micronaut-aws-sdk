package com.agorapulse.micronaut.aws.apigateway.ws.event;

import java.util.Objects;

/**
 * Event which can be used as a parameter of Lambda functions which handles <code>$default</code> or user specified route.
 */
public class WebSocketRequest {

    private RequestContext requestContext;
    private String body;
    private Boolean isBase64Encoded;

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getBase64Encoded() {
        return isBase64Encoded;
    }

    public void setBase64Encoded(Boolean base64Encoded) {
        isBase64Encoded = base64Encoded;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "WebSocketRequest{" +
            "requestContext=" + requestContext +
            ", body='" + body + '\'' +
            ", isBase64Encoded=" + isBase64Encoded +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketRequest that = (WebSocketRequest) o;
        return Objects.equals(requestContext, that.requestContext) &&
            Objects.equals(body, that.body) &&
            Objects.equals(isBase64Encoded, that.isBase64Encoded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestContext, body, isBase64Encoded);
    }
    // CHECKSTYLE:ON
}
