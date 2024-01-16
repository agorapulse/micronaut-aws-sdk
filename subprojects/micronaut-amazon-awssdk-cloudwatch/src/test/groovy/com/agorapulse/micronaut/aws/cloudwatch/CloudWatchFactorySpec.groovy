/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.aws.cloudwatch

import com.agorapulse.micronaut.amazon.awssdk.cloudwatch.CloudWatchConfiguration
import groovy.transform.CompileDynamic
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

@CompileDynamic
class CloudWatchFactorySpec extends Specification {

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.run(
            'aws.cloudwatch.region': 'eu-west-1'
        )
    }

    void 'check configuration present'() {
        expect:
            context.getBean(CloudWatchConfiguration)
    }

}
