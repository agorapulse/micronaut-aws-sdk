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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Event which can be used as a parameter of Lambda functions which handles <code>$connect</code> or <code>$disconnect</code> route.
 */
public class WebSocketConnectionRequest extends WebSocketRequest {

    private Map<String, String> headers;
    private Map<String, List<String>> multiValueHeaders;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public WebSocketConnectionRequest withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Map<String, List<String>> getMultiValueHeaders() {
        return multiValueHeaders;
    }

    public void setMultiValueHeaders(Map<String, List<String>> multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
    }

    public WebSocketConnectionRequest withMultiValueHeaders(Map<String, List<String>> multiValueHeaders) {
        this.multiValueHeaders = multiValueHeaders;
        return this;
    }

    // CHECKSTYLE:OFF
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
            "requestContext=" + getRequestContext() +
            ", body='" + getBody() + '\'' +
            ", isBase64Encoded=" + getIsBase64Encoded() +
            '}';
    }

    // CHECKSTYLE:ON
}
