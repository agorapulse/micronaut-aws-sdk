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
package com.agorapulse.micronaut.aws.lambda;

import com.agorapulse.micronaut.aws.util.ConfigurationUtil;
import com.amazonaws.services.lambda.AWSLambda;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Named;

/**
 * Default configuration for AWS Lambda.
 */
@Primary
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@ConfigurationProperties("aws.lambda")
@Requires(classes = AWSLambda.class)
public class DefaultLambdaConfiguration extends LambdaConfiguration {

}
