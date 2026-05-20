/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.ministack

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import jakarta.inject.Inject

@MicronautTest
@Property(name = 'ministack.enabled', value = 'true')
@Property(name = 'ministack.tag', value = 'latest')
@Property(name = 'ministack.region', value = 'eu-west-1')
@Property(name = 'ministack.access-key', value = 'my-key')
@Property(name = 'ministack.secret-key', value = 'my-secret')
@Property(name = 'ministack.persistence', value = 'true')
@Property(name = 'ministack.env.MINISTACK_FOO', value = 'bar')
@Property(name = 'ministack.env.LAMBDA_EXECUTOR', value = 'local')
class MinistackContainerConfigurationSpec extends Specification {

    @Inject MinistackContainerConfiguration configuration

    void 'configuration is bound from ministack.* properties without hyphenating env keys'() {
        expect:
        configuration.tag == 'latest'
        configuration.region == 'eu-west-1'
        configuration.accessKey == 'my-key'
        configuration.secretKey == 'my-secret'
        configuration.persistence
        configuration.env.get('MINISTACK_FOO') == 'bar'
        configuration.env.get('LAMBDA_EXECUTOR') == 'local'
    }

}
