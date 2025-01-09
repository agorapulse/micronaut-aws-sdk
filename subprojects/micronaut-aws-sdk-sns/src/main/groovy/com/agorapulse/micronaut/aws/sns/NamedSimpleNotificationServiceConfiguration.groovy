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
package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.util.ConfigurationUtil
import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment

/**
 * Named simple queue service configuration for each property key.
 */
@CompileStatic
@EachProperty(value = 'aws.sns.topics', primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@Requires(classes = AmazonSNS, property =  'aws.sns.topics')
class NamedSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {

    final String name

    NamedSimpleNotificationServiceConfiguration(@Parameter String name, Environment environment) {
        super('aws.sns.topics.' + name, environment)
        this.name = name
    }

}
