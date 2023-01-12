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
package com.agorapulse.micronaut.aws.sqs

import com.agorapulse.micronaut.aws.util.ConfigurationUtil
import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

/**
 * Named configuration for simple queue service created for each key in the configuration.
 */
@CompileStatic
@EachProperty(value = 'aws.sqs.queues', primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@Requires(classes = AmazonSQS, property =  'aws.sqs.queues')
class NamedSimpleQueueServiceConfiguration extends SimpleQueueServiceConfiguration {

    final String name

    NamedSimpleQueueServiceConfiguration(@Parameter String name) {
        this.name = name
    }

}
