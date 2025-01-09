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
package com.agorapulse.micronaut.amazon.awssdk.core;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.*;

import jakarta.inject.Singleton;

@Factory
public class AwsConfiguration {

    private static final Region DEFAULT_REGION = Region.EU_WEST_1;

    @Bean
    @Singleton
    AwsCredentialsProvider defaultAwsCredentialsProvider(Environment environment) {
        return AwsCredentialsProviderChain.of(
            EnvironmentAwsCredentialsProvider.create(environment),
            EnvironmentVariableCredentialsProvider.create(),
            SystemPropertyCredentialsProvider.create(),
            ProfileCredentialsProvider.create(),
            ContainerCredentialsProvider.builder().build(),
            InstanceProfileCredentialsProvider.create()
        );
    }

    @Bean
    @Singleton
    AwsRegionProvider defaultAwsRegionProvider(Environment environment) {
        return new AwsRegionProviderChain(
            new EnvironmentAwsRegionProvider(environment),
            new SystemSettingsRegionProvider(),
            new AwsProfileRegionProvider(),
            new InstanceProfileRegionProvider(),
            new BasicAwsRegionProvider(DEFAULT_REGION)
        );
    }

}
