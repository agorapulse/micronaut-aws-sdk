package com.agorapulse.micronaut.aws.apigateway.ws.event;

import java.util.Objects;

/**
 * Request context sent with WebSocket event.
 *
 * The most important part is the {@link #connectionId} which hold the reference which can be used
 * to send message back to connected client and {@link #routeKey} which points to the matched route from the API Gateway.
 */
public class RequestContext {

    private String routeKey;
    private String messageId;
    private EventType eventType;
    private String extendedRequestId;
    private MessageDirection messageDirection;
    private String stage;
    private Long connectedAt;
    private Long requestTimeEpoch;
    private Identity identity;
    private String requestId;
    private String domainName;
    private String connectionId;
    private String apiId;

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getExtendedRequestId() {
        return extendedRequestId;
    }

    public void setExtendedRequestId(String extendedRequestId) {
        this.extendedRequestId = extendedRequestId;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public void setMessageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Long getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Long connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Long getRequestTimeEpoch() {
        return requestTimeEpoch;
    }

    public void setRequestTimeEpoch(Long requestTimeEpoch) {
        this.requestTimeEpoch = requestTimeEpoch;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestContext that = (RequestContext) o;
        return Objects.equals(routeKey, that.routeKey) &&
            Objects.equals(messageId, that.messageId) &&
            eventType == that.eventType &&
            Objects.equals(extendedRequestId, that.extendedRequestId) &&
            messageDirection == that.messageDirection &&
            Objects.equals(stage, that.stage) &&
            Objects.equals(connectedAt, that.connectedAt) &&
            Objects.equals(requestTimeEpoch, that.requestTimeEpoch) &&
            Objects.equals(identity, that.identity) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(domainName, that.domainName) &&
            Objects.equals(connectionId, that.connectionId) &&
            Objects.equals(apiId, that.apiId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeKey, messageId, eventType, extendedRequestId, messageDirection, stage, connectedAt, requestTimeEpoch, identity, requestId, domainName, connectionId, apiId);
    }

    @Override
    public String toString() {
        return "RequestContext{" +
            "routeKey='" + routeKey + '\'' +
            ", messageId='" + messageId + '\'' +
            ", eventType=" + eventType +
            ", extendedRequestId='" + extendedRequestId + '\'' +
            ", messageDirection=" + messageDirection +
            ", stage='" + stage + '\'' +
            ", connectedAt=" + connectedAt +
            ", requestTimeEpoch=" + requestTimeEpoch +
            ", identity='" + identity + '\'' +
            ", requestId='" + requestId + '\'' +
            ", domainName='" + domainName + '\'' +
            ", connectionId='" + connectionId + '\'' +
            ", apiId='" + apiId + '\'' +
            '}';
    }
}
