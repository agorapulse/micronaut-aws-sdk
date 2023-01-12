/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
