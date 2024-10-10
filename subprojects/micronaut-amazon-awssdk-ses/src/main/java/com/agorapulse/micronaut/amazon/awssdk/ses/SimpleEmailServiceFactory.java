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
package com.agorapulse.micronaut.amazon.awssdk.ses;

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.aws.sdk.v2.service.ses.SesClientFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
public class SimpleEmailServiceFactory {

    @Bean(preDestroy = "close")
    @Singleton
    @Replaces(bean = SesClient.class, factory = SesClientFactory.class)
    public SesClient sesClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleEmailServiceConfiguration configuration
    ) {
        return configuration.configure(SesClient.builder(), awsRegionProvider, builderProvider)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean(preDestroy = "close")
    @Singleton
    @Replaces(bean = SesAsyncClient.class, factory = SesClientFactory.class)
    public SesAsyncClient sesAsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        ClientBuilderProvider builderProvider,
        SimpleEmailServiceConfiguration configuration,
        Optional<SdkAsyncHttpClient> httpClient
    ) {
        SesAsyncClientBuilder builder = SesAsyncClient.builder().credentialsProvider(credentialsProvider);
        httpClient.ifPresent(builder::httpClient);
        configuration.configure(builder, awsRegionProvider, builderProvider);
        return builder.build();
    }

    @Bean
    @Singleton
    public SimpleEmailService simpleEmailService(SesClient client, SimpleEmailServiceConfiguration configuration) {
        return new DefaultSimpleEmailService(client, configuration);
    }

}
