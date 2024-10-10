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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.aws.sdk.v2.service.sns.SnsClientFactory;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
public class SimpleNotificationServiceFactory {

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    @Replaces(factory = SnsClientFactory.class, bean = SnsClient.class)
    SnsClient snsClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleNotificationServiceConfiguration configuration
    ) {
        return configuration.configure(SnsClient.builder(), awsRegionProvider, builderProvider)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    @Replaces(factory = SnsClientFactory.class, bean = SnsAsyncClient.class)
    SnsAsyncClient snsAsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleNotificationServiceConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        SnsAsyncClientBuilder builder = SnsAsyncClient.builder().credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider);
        httpClient.ifPresent(builder::httpClient);
        return builder.build();
    }


    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    SimpleNotificationService simpleQueueService(SnsClient sqs, SimpleNotificationServiceConfiguration configuration, ObjectMapper mapper) {
        return new DefaultSimpleNotificationService(sqs, configuration, mapper);
    }

}
