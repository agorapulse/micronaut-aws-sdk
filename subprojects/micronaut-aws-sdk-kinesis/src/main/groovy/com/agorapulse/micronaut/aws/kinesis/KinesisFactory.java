/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonKinesis.class)
public class KinesisFactory {

    @Singleton
    @EachBean(KinesisConfiguration.class)
    AmazonKinesis kinesis(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        KinesisConfiguration configuration
    ) {
        return configuration.configure(AmazonKinesisClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Singleton
    @EachBean(KinesisConfiguration.class)
    KinesisService simpleQueueService(
        AmazonKinesis kinesis,
        KinesisConfiguration configuration,
        ObjectMapper mapper
    ) {
        return new DefaultKinesisService(kinesis, configuration, mapper);
    }

}
