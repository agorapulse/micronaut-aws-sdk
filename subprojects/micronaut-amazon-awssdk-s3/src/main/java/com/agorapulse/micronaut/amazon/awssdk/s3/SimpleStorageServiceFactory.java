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
package com.agorapulse.micronaut.amazon.awssdk.s3;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.aws.sdk.v2.service.s3.S3ClientFactory;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
public class SimpleStorageServiceFactory {

    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    @Replaces(bean = S3Client.class, factory = S3ClientFactory.class)
    public S3Client s3Client(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleStorageServiceConfiguration configuration
    ) {
        return configuration.configure(S3Client.builder(), awsRegionProvider, builderProvider, Optional.empty())
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(configuration.getForcePathStyle())
            .build();
    }
    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    public S3Presigner s3Presigner(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleStorageServiceConfiguration configuration
    ) {
        S3Presigner.Builder builder = configuration.configure(S3Presigner.builder(), awsRegionProvider)
            .credentialsProvider(credentialsProvider);
        if (configuration.getForcePathStyle() != null) {
            builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(configuration.getForcePathStyle())
                .build());
        }
        return builder.build();
    }

    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    @Replaces(bean = S3AsyncClient.class, factory = S3ClientFactory.class)
    public S3AsyncClient s3AsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleStorageServiceConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder()
            .forcePathStyle(configuration.getForcePathStyle())
            .credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider, builderProvider, httpClient);
        return builder.build();
    }

    @Singleton
    @EachBean(SimpleStorageServiceConfiguration.class)
    public SimpleStorageService simpleStorageService(
        S3Client s3,
        S3Presigner presigner,
        SimpleStorageServiceConfiguration configuration
    ) {
        return new DefaultSimpleStorageService(configuration.getBucket(), s3, presigner);
    }

}
