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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.aws.sdk.v2.service.sqs.SqsClientFactory;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = SqsClient.class)

public class SimpleQueueServiceFactory {

    @Singleton
    @EachBean(SimpleQueueServiceConfiguration.class)
    @Replaces(bean = SqsClient.class, factory = SqsClientFactory.class)
    SqsClient sqsClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleQueueServiceConfiguration configuration
    ) {
        return configuration.configure(SqsClient.builder(), awsRegionProvider, builderProvider, Optional.empty())
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Singleton
    @EachBean(SimpleQueueServiceConfiguration.class)
    @Replaces(bean = SqsAsyncClient.class, factory = SqsClientFactory.class)
    SqsAsyncClient sqsAsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleQueueServiceConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        SqsAsyncClientBuilder builder = SqsAsyncClient.builder().credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider, httpClient);
        return builder.build();
    }

    @Singleton
    @EachBean(SimpleQueueServiceConfiguration.class)
    SimpleQueueService simpleQueueService(SqsClient sqs, SimpleQueueServiceConfiguration configuration) {
        return new DefaultSimpleQueueService(sqs, configuration);
    }

}
