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
package com.agorapulse.micronaut.aws.sts;

import com.agorapulse.micronaut.aws.util.AWSClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;

@Factory
@Requires(classes = AWSSecurityTokenService.class)
public class SecurityTokenServiceFactory {

    @Bean
    @Singleton
    AWSSecurityTokenService awsSecurityTokenService(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SecurityTokenServiceConfiguration configuration
    ) {
        return configuration.configure(AWSSecurityTokenServiceClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

}
