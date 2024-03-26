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
package com.agorapulse.micronaut.amazon.awssdk.lambda.itest

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest                                                                          // <1>
class TestAwsLambdaRuntimeSpec extends Specification {

    private static final String RESPONSE_TEXT = 'olleH'
    private static final String REQUEST = '{ "message": "Hello" }'

    @Inject ObjectMapper mapper

    @Inject TestAwsLambdaRuntime aws                                                    // <2>

    @MockBean(DefaultSomeService) SomeService someService() {                           // <3>
        return Mock(SomeService) {
            transform(_ as String) >> { String msg -> msg.reverse() }
        }
    }

    void 'handle request'() {                                                           // <4>
        expect:
            aws.handleRequest(PojoHandler, REQUEST).text == RESPONSE_TEXT
            aws.handleRequest(PojoHandler, new ByteArrayInputStream(REQUEST.bytes)).text == RESPONSE_TEXT

            aws.apply(FunctionHandler, REQUEST).text == RESPONSE_TEXT
            aws.apply(FunctionHandler, new ByteArrayInputStream(REQUEST.bytes)).text == RESPONSE_TEXT

            aws.<Response>invoke(PojoHandler.name, REQUEST).text == RESPONSE_TEXT
            aws.<Response>invoke(PojoHandler.name, new ByteArrayInputStream(REQUEST.bytes)).text == RESPONSE_TEXT

            aws.<Response>invoke(SimpleHandler.name + '::execute', REQUEST).text == RESPONSE_TEXT
            aws.<Response>invoke(SimpleHandler.name + '::execute', REQUEST).text == RESPONSE_TEXT

            mapper.readValue(aws.stream(PojoStreamHandler, REQUEST).toString(), Response).text == RESPONSE_TEXT
            mapper.readValue(aws.stream(PojoStreamHandler, new ByteArrayInputStream(REQUEST.bytes)).toString(), Response).text == RESPONSE_TEXT

            mapper.readValue(aws.stream(PojoStreamHandler.name, REQUEST).toString(), Response).text == RESPONSE_TEXT
            mapper.readValue(aws.stream(PojoStreamHandler.name, new ByteArrayInputStream(REQUEST.bytes)).toString(), Response).text == RESPONSE_TEXT
    }

}
