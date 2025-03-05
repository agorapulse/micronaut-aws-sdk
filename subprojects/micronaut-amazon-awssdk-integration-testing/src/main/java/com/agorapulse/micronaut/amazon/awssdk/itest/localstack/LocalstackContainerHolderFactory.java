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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack;

import com.agorapulse.micronaut.amazon.awssdk.core.AwsConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Replaces(AwsConfiguration.class)
@Requires(missingProperty = "localstack.disabled")
public class LocalstackContainerHolderFactory {

    @Primary
    @Singleton
    @Bean(preDestroy = "close")
    @Requires(property = "localstack.shared", notEquals = "true")
    public LocalstackContainerHolder localstackContainerHolderLazy(
        LocalstackContainerConfiguration configuration,
        List<LocalstackContainerOverridesConfiguration> overrides
    ) {
        return new LocalstackContainerHolder(configuration, overrides);
    }

    @Primary
    @Context
    @Bean(preDestroy = "close")
    @Requires(property = "locastack.shared", value = "true")
    public LocalstackContainerHolder localstackContainerHolderEager(
        LocalstackContainerConfiguration configuration,
        List<LocalstackContainerOverridesConfiguration> overrides
    ) {
        return new LocalstackContainerHolder(configuration, overrides);
    }

}
