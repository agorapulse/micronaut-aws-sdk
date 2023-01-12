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
package com.agorapulse.micronaut.amazon.awssdk.cloudwatchlogs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Factory for providing CloudWatch Logs.
 *
 * This is very basic support for CloudWatch Logs which main purpose is to support reading Lambda logs in the tests.
 */
@Factory
@Requires(classes = CloudWatchLogsClient.class)
public class CloudWatchLogsFactory {

    @Bean
    @Singleton
    CloudWatchLogsClient cloudWatchLogs(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        CloudWatchLogsConfiguration configuration
    ) {
        return configuration
            .configure(CloudWatchLogsClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider)
            .build();
    }


    @Bean
    @Singleton
    CloudWatchLogsAsyncClient cloudWatchAsync(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        CloudWatchLogsConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        CloudWatchLogsAsyncClientBuilder builder = configuration
            .configure(CloudWatchLogsAsyncClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider);
        httpClient.ifPresent(builder::httpClient);
        return builder.build();
    }

}
