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
package com.agorapulse.micronaut.aws.apigateway.ws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Message sender sends messages to WebSocket's clients
 */
public interface MessageSender {

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    default void send(String connectionId, String payload) {
        send(connectionId, new ByteArrayInputStream(payload.getBytes()));
    }

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    void send(String connectionId, InputStream payload);

    /**
     * Send a message to client with given connection ID.
     * @param connectionId connection ID of the client
     * @param payload the payload to be sent
     */
    void send(String connectionId, Object payload);

}
