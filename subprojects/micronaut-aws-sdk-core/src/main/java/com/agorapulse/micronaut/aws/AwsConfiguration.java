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
package com.agorapulse.micronaut.aws;

import com.agorapulse.micronaut.aws.util.EnvironmentAWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.AwsEnvVarOverrideRegionProvider;
import com.amazonaws.regions.AwsProfileRegionProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.AwsSystemPropertyRegionProvider;
import com.amazonaws.regions.InstanceMetadataRegionProvider;
import com.amazonaws.regions.Regions;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;

import jakarta.inject.Singleton;

@Factory
public class AwsConfiguration {

    private static final Regions DEFAULT_REGION = Regions.EU_WEST_1;

    @Bean
    @Singleton
    AWSCredentialsProvider defaultAwsCredentialsProvider(Environment environment) {
        return new AWSCredentialsProviderChain(
            new EnvironmentAWSCredentialsProvider(environment),
            new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider(),
            new ProfileCredentialsProvider(),
            new EC2ContainerCredentialsProviderWrapper()
        );
    }

    @Bean
    @Singleton
    AwsRegionProvider defaultAwsRegionProvider(Environment environment) {
        return new SafeAwsRegionProviderChain(
            new EnvironmentAwsRegionProvider(environment),
            new AwsEnvVarOverrideRegionProvider(),
            new AwsSystemPropertyRegionProvider(),
            new AwsProfileRegionProvider(),
            new InstanceMetadataRegionProvider(),
            new BasicAwsRegionProvider(DEFAULT_REGION.getName())
        );
    }

}
