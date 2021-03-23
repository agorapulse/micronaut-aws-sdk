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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.env.Environment;

/**
 * Named simple queue service configuration for each property key.
 */
@EachProperty(value = "aws.sns.topics", primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
public class NamedSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {
    public NamedSimpleNotificationServiceConfiguration(@Parameter String name, Environment environment) {
        super("aws.sns.topics." + name, environment);
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    private final String name;
}
