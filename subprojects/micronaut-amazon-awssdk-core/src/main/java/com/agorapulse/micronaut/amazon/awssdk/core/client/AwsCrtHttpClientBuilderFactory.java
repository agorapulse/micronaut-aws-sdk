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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

@Factory
@Requires(classes = {AwsCrtHttpClient.class, AwsCrtAsyncHttpClient.class})
public class AwsCrtHttpClientBuilderFactory {

    @Bean
    @Singleton
    @Named("aws-crt")
    public AwsCrtAsyncHttpClient.Builder awsCrtHttpAsyncClientBuilder() {
        return AwsCrtAsyncHttpClient.builder();
    }

    @Bean
    @Singleton
    @Named("aws-crt")
    public AwsCrtHttpClient.Builder awsCrtHttpClientBuilder() {
        return AwsCrtHttpClient.builder();
    }

}
