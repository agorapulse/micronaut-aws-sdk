/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.sts;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = StsClient.class)
public class SecurityTokenServiceFactory {

    @Bean
    @Singleton
    StsClient awsSecurityTokenService(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SecurityTokenServiceConfiguration configuration
    ) {
        return configuration
            .configure(StsClient.builder().credentialsProvider(credentialsProvider), awsRegionProvider)
            .build();
    }

    @Bean
    @Singleton
    StsAsyncClient awsSecurityTokenAsyncService(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SecurityTokenServiceConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        StsAsyncClientBuilder builder = StsAsyncClient.builder().credentialsProvider(credentialsProvider);
        configuration.configure(builder, awsRegionProvider);
        httpClient.ifPresent(builder::httpClient);
        return builder.build();
    }

}
