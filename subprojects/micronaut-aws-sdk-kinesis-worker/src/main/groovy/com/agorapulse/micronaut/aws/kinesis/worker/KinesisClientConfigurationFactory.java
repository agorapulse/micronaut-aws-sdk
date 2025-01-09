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
package com.agorapulse.micronaut.aws.kinesis.worker;

import com.agorapulse.micronaut.aws.util.AWSClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import jakarta.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = KinesisClientLibConfiguration.class)
public class KinesisClientConfigurationFactory {

    @Singleton
    @Requires(property = "aws.kinesis")
    @EachBean(KinesisClientConfiguration.class)
    KinesisClientLibConfiguration kinesisClientLibConfiguration(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        KinesisClientConfiguration configuration,
        @Value("${aws.kinesis.region}") Optional<String> region
    ) {
        return configuration.getKinesisClientLibConfiguration(
            clientConfiguration.getClientConfiguration(),
            credentialsProvider,
            region.orElseGet(awsRegionProvider::getRegion)
        );
    }

}
