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
package com.agorapulse.micronaut.aws.cloudwatchlogs;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.logs.*;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;

/**
 * Factory for providing CloudWatch Logs.
 *
 * This is very basic support for CloudWatch Logs which main purpose is to support reading Lambda logs in the tests.
 */
@Factory
@Requires(classes = AWSLogsClient.class)
public class CloudWatchLogsFactory {

    @Bean
    @Singleton
    AWSLogs cloudWatchLogs(
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        CloudWatchLogsConfiguration configuration
    ) {
        return configuration
            .configure(AWSLogsClient.builder().withCredentials(credentialsProvider), awsRegionProvider)
            .build();
    }


    @Bean
    @Singleton
    AWSLogsAsync cloudWatchAsync(
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        CloudWatchLogsConfiguration configuration
    ) {
        return configuration
            .configure(AWSLogsAsyncClient.asyncBuilder().withCredentials(credentialsProvider), awsRegionProvider)
            .build();
    }

}
