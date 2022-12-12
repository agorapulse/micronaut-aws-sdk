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
package com.agorapulse.micronaut.amazon.awssdk.lambda

import io.micronaut.test.annotation.MicronautTest

import javax.inject.Inject

@MicronautTest                                                                          // <1>
class HelloClientSpec extends AbstractClientSpec {

    @Inject HelloClient client                                                          // <2>

    void 'execute function code'() {
        expect:
            client.hello('Vlad').message == 'Hello Vlad'                                // <3>
    }

}