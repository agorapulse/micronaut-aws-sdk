/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

/**
 * Factory class which provides the {@link AmazonDynamoDB} and {@link IDynamoDBMapper} into the application context.
 */
@Factory
@Requires(classes = DynamoDB.class)
public class DynamoDBFactory {

    @Bean
    @Singleton
    @Requires(missingProperty = "aws.dax.endpoint")
    AmazonDynamoDB amazonDynamoDB(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        DynamoDBConfiguration configuration
    ) {
        return configuration.configure(AmazonDynamoDBClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Bean
    @Singleton
    IDynamoDBMapper dynamoDBMapper(AmazonDynamoDB client) {
        return new DynamoDBMapper(client);
    }

}
