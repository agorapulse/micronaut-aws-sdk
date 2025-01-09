/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.core.client;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

import java.util.Optional;

public interface ClientBuilderProvider {

    String APACHE = "apache";
    String AWS_CRT = "aws-crt";
    String URL_CONNECTION = "url-connection";
    String NETTY = "netty";

    <B extends SdkHttpClient.Builder<B>> Optional<SdkHttpClient.Builder<B>> findHttpClientBuilder(String implementation);

    <B extends SdkAsyncHttpClient.Builder<B>> Optional<SdkAsyncHttpClient.Builder<B>> findAsyncHttpClientBuilder(String implementation);

}
