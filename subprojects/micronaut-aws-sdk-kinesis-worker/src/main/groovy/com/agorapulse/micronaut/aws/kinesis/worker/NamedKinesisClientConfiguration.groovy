/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.kinesis.worker

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value

/**
 * Kinesis listener configuration for given configuration key.
 */
@CompileStatic
@EachProperty('aws.kinesis.listeners')
@Requires(classes = KinesisClientLibConfiguration)
class NamedKinesisClientConfiguration extends KinesisClientConfiguration {

    final String name

    NamedKinesisClientConfiguration(
        @Parameter String name,
        @Value('${aws.kinesis.application.name:}') String applicationName,
        @Value('${aws.kinesis.worker.id:}') String workerId
    ) {
        super(applicationName, workerId)
        this.name = name
    }

}
