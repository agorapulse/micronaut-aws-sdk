/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.lambda;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClientBuilder;
import software.amazon.awssdk.services.lambda.LambdaClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = LambdaClient.class)
public class LambdaClientFactory {

    @Bean
    @Singleton
    @EachBean(LambdaConfiguration.class)
    LambdaClient lambdaClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        LambdaConfiguration configuration
    ) {
        return configuration.configure(LambdaClient.builder(), awsRegionProvider, builderProvider, Optional.empty())
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean
    @Singleton
    @EachBean(LambdaConfiguration.class)
    LambdaAsyncClient lambdaAsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        LambdaConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        LambdaAsyncClientBuilder builder = LambdaAsyncClient.builder().credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider, httpClient);
        return builder.build();
    }

}
