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
package com.agorapulse.micronaut.aws.s3;

import com.agorapulse.micronaut.aws.util.AWSClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;

@Factory
@Requires(classes = AmazonS3.class)
public class SimpleStorageServiceFactory {

    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    AmazonS3 amazonS3(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleStorageServiceConfiguration configuration
    ) {
        return configuration.configure(AmazonS3ClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withPathStyleAccessEnabled(configuration.getPathStyleAccessEnabled())
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    SimpleStorageService simpleStorageService(AmazonS3 s3, SimpleStorageServiceConfiguration configuration) {
        return new DefaultSimpleStorageService(s3, configuration.getBucket());
    }

}
