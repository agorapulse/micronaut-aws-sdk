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
package com.agorapulse.micronaut.amazon.awssdk.cloudwatch;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import jakarta.inject.Singleton;
import java.util.Optional;

/**
 * Factory for providing CloudWatch.
 *
 * This is very basic support for CloudWatch which main purpose is to support Kinesis listeners customisation.
 */
@Factory
@Requires(classes = CloudWatchClient.class)
public class CloudWatchFactory {

    @Bean
    @Singleton
    CloudWatchClient cloudWatch(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        CloudWatchConfiguration configuration
    ) {
        return configuration
            .configure(CloudWatchClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider, builderProvider)
            .build();
    }


    @Bean
    @Singleton
    CloudWatchAsyncClient cloudWatchAsync(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        CloudWatchConfiguration configuration,
        ClientBuilderProvider builderProvider,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        CloudWatchAsyncClientBuilder builder = configuration
            .configure(CloudWatchAsyncClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider, builderProvider);
        httpClient.ifPresent(builder::httpClient);
        return builder.build();
    }

}
