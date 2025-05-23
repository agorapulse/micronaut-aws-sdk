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
package com.agorapulse.micronaut.aws.sns;

import com.agorapulse.micronaut.aws.util.AWSClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;

@Factory
@Requires(classes = AmazonSNS.class)
public class SimpleNotificationServiceFactory {

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    AmazonSNS amazonSNS(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleNotificationServiceConfiguration configuration
    ) {
        return configuration.configure(AmazonSNSClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    SimpleNotificationService simpleQueueService(AmazonSNS sqs, SimpleNotificationServiceConfiguration configuration, ObjectMapper mapper) {
        return new DefaultSimpleNotificationService(sqs, configuration, mapper);
    }

}
