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
package com.agorapulse.micronaut.aws.kinesis

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests for Kinesis configuration setup.
 */
class KinesisConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'one service present by default'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(KinesisService).size() == 1
            context.getBean(KinesisConfiguration).stream == ''
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.stream': 'StreamName'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 1
            context.getBean(KinesisConfiguration).stream == 'StreamName'
            context.getBean(KinesisService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.streams.sample.stream': 'SampleStream'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 2
            context.getBean(KinesisService)
            context.getBean(KinesisService, Qualifiers.byName('default'))
            context.getBean(KinesisService, Qualifiers.byName('sample'))
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.stream': 'StreamName',
                'aws.kinesis.streams.sample.stream': 'SampleStream'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 2
            context.getBean(KinesisService, Qualifiers.byName('default'))
            context.getBean(KinesisService, Qualifiers.byName('sample'))
    }

}
