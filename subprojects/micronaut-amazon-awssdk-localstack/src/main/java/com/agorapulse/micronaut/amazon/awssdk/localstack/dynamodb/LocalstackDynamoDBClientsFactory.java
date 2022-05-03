/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.localstack.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBClientsFactory;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import javax.inject.Singleton;

/**
 * Factory which replaces {@link com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBClientsFactory}
 * to provide clients backed by Localstack container.
 */
@Factory
@Replaces(DynamoDBClientsFactory.class)
@Requires(classes = DynamoDBClientsFactory.class)
public class LocalstackDynamoDBClientsFactory {

    private final LocalstackContainerHolder holder;

    public LocalstackDynamoDBClientsFactory(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.DYNAMODB);
    }

    @Primary
    @Singleton
    @Bean(preDestroy = "close")
    public DynamoDbClient dynamoDbClient(
        DynamoDBConfiguration configuration
    ) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().credentialsProvider(holder);
        configuration.configure(builder, holder);
        builder.endpointOverride(holder.getEndpointOverride(LocalStackContainer.Service.DYNAMODB));
        return builder.build();
    }

    @Primary
    @Singleton
    @Bean(preDestroy = "close")
    public DynamoDbAsyncClient dynamoDbAsyncClient(
        DynamoDBConfiguration configuration
    ) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder().credentialsProvider(holder);
        configuration.configure(builder, holder);
        builder.endpointOverride(holder.getEndpointOverride(LocalStackContainer.Service.DYNAMODB));
        return builder.build();
    }

}
