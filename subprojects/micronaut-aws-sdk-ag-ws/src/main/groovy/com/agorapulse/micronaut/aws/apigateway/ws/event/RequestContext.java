/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public RequestContext withRouteKey(String routeKey) {
        this.routeKey = routeKey;
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public RequestContext withMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public RequestContext withEventType(EventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getExtendedRequestId() {
        return extendedRequestId;
    }

    public void setExtendedRequestId(String extendedRequestId) {
        this.extendedRequestId = extendedRequestId;
    }

    public RequestContext withExtendedRequestId(String extendedRequestId) {
        this.extendedRequestId = extendedRequestId;
        return this;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public void setMessageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
    }

    public RequestContext withMessageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
        return this;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public RequestContext withStage(String stage) {
        this.stage = stage;
        return this;
    }

    public Long getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Long connectedAt) {
        this.connectedAt = connectedAt;
    }

    public RequestContext withConnectedAt(Long connectedAt) {
        this.connectedAt = connectedAt;
        return this;
    }

    public Long getRequestTimeEpoch() {
        return requestTimeEpoch;
    }

    public void setRequestTimeEpoch(Long requestTimeEpoch) {
        this.requestTimeEpoch = requestTimeEpoch;
    }

    public RequestContext withRequestTimeEpoch(Long requestTimeEpoch) {
        this.requestTimeEpoch = requestTimeEpoch;
        return this;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public RequestContext withIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public RequestContext withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public RequestContext withDomainName(String domainName) {
        this.domainName = domainName;
        return this;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public RequestContext withConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public RequestContext withApiId(String apiId) {
        this.apiId = apiId;
        return this;
    }

    // CHECKSTYLE:OFF
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
    // CHECKSTYLE:ON
}
