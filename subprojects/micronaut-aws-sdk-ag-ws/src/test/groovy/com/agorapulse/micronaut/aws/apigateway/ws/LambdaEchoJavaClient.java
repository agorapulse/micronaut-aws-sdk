/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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

import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketRequest;
import com.agorapulse.micronaut.aws.apigateway.ws.event.WebSocketResponse;
import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.function.client.FunctionClient;
import org.reactivestreams.Publisher;

import javax.inject.Named;

@FunctionClient
interface LambdaEchoJavaClient {

    @SingleResult
    @Named("lambda-echo-java")
    Publisher<WebSocketResponse> lambdaEcho(WebSocketRequest event);

}
