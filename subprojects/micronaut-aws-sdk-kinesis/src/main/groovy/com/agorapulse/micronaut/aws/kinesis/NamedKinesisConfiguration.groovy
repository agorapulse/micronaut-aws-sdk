/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.aws.kinesis

import com.agorapulse.micronaut.aws.util.ConfigurationUtil
import com.amazonaws.services.kinesis.AmazonKinesis
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

/**
 * Named Kinesis configuration, published with named qualifier of the same name as is the key of this configuration.
 */
@CompileStatic
@EachProperty(value = 'aws.kinesis.streams', primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@Requires(classes = AmazonKinesis)
class NamedKinesisConfiguration extends KinesisConfiguration {

    final String name

    NamedKinesisConfiguration(@Parameter String name) {
        this.name = name
    }

}
