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
package com.agorapulse.micronaut.aws.dynamodb;

import com.agorapulse.micronaut.aws.util.AWSClientConfiguration;
import com.amazon.dax.client.dynamodbv2.AmazonDaxClient;
import com.amazon.dax.client.dynamodbv2.AmazonDaxClientBuilder;
import com.amazon.dax.client.dynamodbv2.ClientConfig;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Factory class which provides the {@link AmazonDynamoDB} instance backed by {@link AmazonDaxClient}.
 */
@Factory
@Requires(classes = AmazonDaxClient.class)
public class DynamoDBDaxFactory {

    @Bean
    @Singleton
    @Requires(property = "aws.dax.endpoint")
    AmazonDynamoDB amazonDynamoDB(
        @Value("${aws.dax.endpoint}") String endpoint,
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.dax.region}") Optional<String> region
    ) {
        ClientConfig clientConfig = migrateClientConfig(clientConfiguration.getClientConfiguration())
            .withEndpoints(endpoint)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withCredentialsProvider(credentialsProvider);
        return AmazonDaxClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfig)
            .build();
    }

    private ClientConfig migrateClientConfig(ClientConfiguration clientConfiguration) {
        return new ClientConfig()
            .withConnectTimeout(clientConfiguration.getConnectionTimeout(), TimeUnit.MILLISECONDS)
            .withMaxPendingConnectsPerHost(clientConfiguration.getMaxConnections())
            .withReadRetries(clientConfiguration.getMaxErrorRetry())
            .withWriteRetries(clientConfiguration.getMaxErrorRetry());
    }
}
