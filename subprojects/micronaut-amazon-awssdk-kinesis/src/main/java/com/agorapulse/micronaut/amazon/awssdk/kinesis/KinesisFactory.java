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
package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = KinesisClient.class)
public class KinesisFactory {

    @Singleton
    @Bean(preDestroy = "close")
    @EachBean(KinesisConfiguration.class)
    KinesisClient kinesis(
        KinesisConfiguration configuration,
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider
    ) {
        return configuration.configure(KinesisClient.builder(), awsRegionProvider, builderProvider, Optional.empty())
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Singleton
    @Bean(preDestroy = "close")
    @EachBean(KinesisConfiguration.class)
    KinesisAsyncClient kinesisAsync(
        KinesisConfiguration configuration,
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder().credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider, httpClient);
        return builder.build();
    }

    @Singleton
    @EachBean(KinesisConfiguration.class)
    KinesisService simpleQueueService(
        KinesisClient kinesis,
        KinesisConfiguration configuration,
        ObjectMapper mapper
    ) {
        return new DefaultKinesisService(kinesis, configuration, mapper);
    }

}
