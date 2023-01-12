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
package com.agorapulse.micronaut.aws.apigateway.ws.event;

/**
 * Type of the WebSocket event.
 */
public enum EventType {
    /**
     * Type of event sent when the client is connected.
     */
    CONNECT,

    /**
     * Type of event sent when the incoming message arrives.
     */
    MESSAGE,

    /**
     * Type of event sent when the client disconnects.
     *
     * There is no guarantee this event is triggered.
     */
    DISCONNECT
}
