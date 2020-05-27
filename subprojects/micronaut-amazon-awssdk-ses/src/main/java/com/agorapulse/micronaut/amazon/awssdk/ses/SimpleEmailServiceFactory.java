/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.SesClient;

import javax.inject.Singleton;

@Factory
public class SimpleEmailServiceFactory {

    @Bean(preDestroy = "close")
    @Singleton
    public SesClient sesClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleEmailServiceConfiguration configuration
    ) {
        return configuration.configure(SesClient.builder(), awsRegionProvider)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean(preDestroy = "close")
    @Singleton
    public SesAsyncClient sesAsyncClient(
        AwsCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleEmailServiceConfiguration configuration
    ) {
        return configuration.configure(SesAsyncClient.builder(), awsRegionProvider)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean
    @Singleton
    public SimpleEmailService simpleEmailService(SesClient client) {
        return new DefaultSimpleEmailService(client);
    }

}
