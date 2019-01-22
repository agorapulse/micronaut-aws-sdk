package com.agorapulse.micronaut.aws.apigateway.ws.event;

import java.util.Objects;

/**
 * Simplified Identity of the WebSocket containing just the source IP address.
 */
public class Identity {

    private String sourceIp;

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public Identity withSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
        return this;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "Identity{" +
            "sourceIp='" + sourceIp + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return Objects.equals(sourceIp, identity.sourceIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceIp);
    }
    // CHECKSTYLE:ON
}
