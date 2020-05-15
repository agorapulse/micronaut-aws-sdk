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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.env.Environment;

import javax.inject.Named;

/**
 * Default simple queue service configuration.
 */
@Primary
@Named("default")
@ConfigurationProperties("aws.sns")
public class DefaultSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {

    public DefaultSimpleNotificationServiceConfiguration(Environment environment) {
        super("aws.sns", environment);
    }

}
