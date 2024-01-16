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
package com.agorapulse.micronaut.aws.kinesis

import com.amazonaws.services.kinesis.model.CreateStreamResult
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Retry
import spock.lang.Specification

import javax.inject.Inject

/**
 * Tests for kinesis service.
 */
// tag::spec[]
@MicronautTest                                                                          // <1>
class KinesisDemoSpec extends Specification {

    @Inject KinesisService service                                                      // <2>

    @Retry
    void 'new default stream'() {
        when:
            CreateStreamResult stream = service.createStream('NewStream')
        then:
            stream
    }

}
// end::spec[]
