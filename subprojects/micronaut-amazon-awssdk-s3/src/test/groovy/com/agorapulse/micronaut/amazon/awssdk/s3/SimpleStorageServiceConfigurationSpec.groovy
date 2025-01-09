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
package com.agorapulse.micronaut.amazon.awssdk.s3

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
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
            context.getBeanDefinitions(SimpleStorageServiceConfiguration).size() == 1
            context.getBeanDefinitions(SimpleStorageService).size() == 1
            context.getBeanDefinitions(S3Client).size() == 1
            context.getBeanDefinitions(S3AsyncClient).size() == 1
            context.getBeanDefinitions(S3Presigner).size() == 1
            context.getBean(SimpleStorageServiceConfiguration).bucket == null
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
            context.getBean(S3AsyncClient)
            context.getBean(S3Presigner)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.buckets.samplebucket.bucket': 'bucket.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 2
            context.getBean(SimpleStorageService)
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
            context.getBean(NamedSimpleStorageServiceConfiguration).name == 'samplebucket'
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.bucket': 'default.example.com',
                'aws.s3.buckets.samplebucket.bucket': 'sample.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 2
            context.getBean(SimpleStorageService)
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
            context.getBean(SimpleStorageServiceConfiguration) instanceof DefaultSimpleStorageServiceConfiguration
            context.getBean(SimpleStorageServiceConfiguration, Qualifiers.byName('samplebucket')) instanceof NamedSimpleStorageServiceConfiguration
            context.getBean(SimpleStorageServiceConfiguration, Qualifiers.byName('samplebucket')).name == 'samplebucket'
    }

}
