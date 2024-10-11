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
package com.agorapulse.micronaut.amazon.awssdk.core.client

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class ClientBuilderProviderSpec extends Specification {

    @Inject ClientBuilderProvider provider

    void 'known providers found'() {
        expect:
            verifyAll(provider) {
                findHttpClientBuilder(ClientBuilderProvider.APACHE).present
                findHttpClientBuilder(ClientBuilderProvider.AWS_CRT).present
                findHttpClientBuilder(ClientBuilderProvider.URL_CONNECTION).present

                findAsyncHttpClientBuilder(ClientBuilderProvider.NETTY).present
                findAsyncHttpClientBuilder(ClientBuilderProvider.AWS_CRT).present
            }
    }

}
