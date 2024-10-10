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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.aws.sdk.v2.service.dynamodb.DynamoDbClientFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import jakarta.inject.Singleton;
import java.util.Optional;

/**
 * Factory class which provides the {@link software.amazon.awssdk.services.dynamodb.DynamoDbClient} and
 * {@link software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient} beans.
 */
@Factory
public class DynamoDBClientsFactory {

    @Bean(preDestroy = "close")
    @Singleton
    @Replaces(bean = DynamoDbClient.class, factory = DynamoDbClientFactory.class)
    public DynamoDbClient dynamoDbClient(
        DynamoDBConfiguration configuration,
        AwsCredentialsProvider awsCredentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider
    ) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().credentialsProvider(awsCredentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider);
        return builder.build();
    }

    @Bean(preDestroy = "close")
    @Singleton
    @Replaces(bean = DynamoDbAsyncClient.class, factory = DynamoDbClientFactory.class)
    public DynamoDbAsyncClient dynamoDbAsyncClient(
        DynamoDBConfiguration configuration,
        AwsCredentialsProvider awsCredentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder().credentialsProvider(awsCredentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider);
        httpClient.ifPresent(builder::httpClient);
        return builder.build();
    }

}
