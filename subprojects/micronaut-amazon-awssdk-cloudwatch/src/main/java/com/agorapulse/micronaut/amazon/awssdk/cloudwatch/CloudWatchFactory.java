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
package com.agorapulse.micronaut.amazon.awssdk.cloudwatch;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import javax.inject.Singleton;

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
        CloudWatchConfiguration configuration
    ) {
        return configuration
            .configure(CloudWatchClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider)
            .build();
    }

}
