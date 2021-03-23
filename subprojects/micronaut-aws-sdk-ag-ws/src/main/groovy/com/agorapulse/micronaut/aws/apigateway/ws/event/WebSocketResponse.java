/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * WebSocket response which always needs to be sent back from the function handling any of API Gateway WebSocket
 * proxy events.
 */
public class WebSocketResponse {

    public static final WebSocketResponse OK = new WebSocketResponse(200);
    public static final WebSocketResponse ERROR = new WebSocketResponse(500);

    private final Integer statusCode;

    @ConstructorProperties("statusCode")
    public WebSocketResponse(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    // CHECKSTYLE:OFF
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
    // CHECKSTYLE:ON
}
