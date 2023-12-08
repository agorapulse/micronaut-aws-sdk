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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import jakarta.inject.Inject

@MicronautTest
@Property(name = 'localstack.env.DEBUG', value = '1')
@Property(name = 'localstack.env.LAMBDA_EXECUTOR', value = 'local')
class LocalstackContainerConfigurationSpec extends Specification {

    @Inject LocalstackContainerConfiguration configuration

    void 'env map should not apply hyphenated conversion to the key values'() {
        expect:
        configuration.env.get('DEBUG') == '1'
        configuration.env.get('LAMBDA_EXECUTOR') == 'local'
    }

}
