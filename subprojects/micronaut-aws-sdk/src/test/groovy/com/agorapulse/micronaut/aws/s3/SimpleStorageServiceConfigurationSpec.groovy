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
package com.agorapulse.micronaut.aws.s3

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Test for simple storage service auto-configuration.
 */
class SimpleStorageServiceConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'one service present by default'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 1
            context.getBean(SimpleStorageServiceConfiguration).bucket == ''
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.bucket': 'bucket.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 1
            context.getBean(SimpleStorageServiceConfiguration).bucket == 'bucket.example.com'
            context.getBean(SimpleStorageService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.buckets.samplebucket.bucket': 'bucket.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 2
            context.getBean(SimpleStorageService)
            context.getBean(SimpleStorageService, Qualifiers.byName('default'))
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.bucket': 'default.example.com',
                'aws.s3.buckets.samplebucket.bucket': 'sample.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 2
            context.getBean(SimpleStorageService, Qualifiers.byName('default'))
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
    }

}
