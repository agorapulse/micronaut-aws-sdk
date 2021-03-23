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
package com.agorapulse.micronaut.aws.apigateway.ws.event

import spock.lang.Specification

/**
 * Sanity checks for web socket event.
 */
class WebSocketResponseSpec extends Specification {

    void 'sanity check'() {
        expect:
            WebSocketResponse.OK == new WebSocketResponse(200)
            WebSocketResponse.OK.toString() == new WebSocketResponse(200).toString()
            WebSocketResponse.OK.hashCode() == new WebSocketResponse(200).hashCode()

            WebSocketResponse.OK.statusCode == 200
            WebSocketResponse.ERROR.statusCode == 500
    }

}
