/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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

import com.amazonaws.regions.AwsRegionProvider;
import io.micronaut.context.env.Environment;

/**
 * A {@link AwsRegionProvider} that reads from the {@link Environment}.
 *
 * @since 1.0.0
 */
public class EnvironmentAwsRegionProvider extends AwsRegionProvider {

    /**
     * Environment variable name for the AWS access key ID.
     */
    public static final String REGION_ENV_VAR = "aws.region";

    private final Environment environment;

    /**
     * Constructor.
     * @param environment environment
     */
    public EnvironmentAwsRegionProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getRegion() {
        return environment.getProperty(REGION_ENV_VAR, String.class, (String) null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
