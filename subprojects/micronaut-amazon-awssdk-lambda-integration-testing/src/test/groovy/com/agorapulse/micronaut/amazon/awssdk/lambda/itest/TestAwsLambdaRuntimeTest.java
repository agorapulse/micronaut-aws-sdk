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
package com.agorapulse.micronaut.amazon.awssdk.lambda.itest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import spock.lang.Specification;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@MicronautTest                                                                          // <1>
public class TestAwsLambdaRuntimeTest extends Specification {

    private static final String RESPONSE_TEXT = "olleH";
    private static final String REQUEST = "{ \"message\": \"Hello\" }";

    @Inject ObjectMapper mapper;

    @Inject TestAwsLambdaRuntime aws;                                                   // <2>

    @MockBean(DefaultSomeService.class) SomeService someService() {                     // <3>
        SomeService mock = Mockito.mock(SomeService.class);
        when(mock.transform("Hello")).thenReturn(RESPONSE_TEXT);
        return mock;
    }

    @Test
    void handleRequest() throws Exception {                                             // <4>
        assertEquals(
            RESPONSE_TEXT,
            aws.handleRequest(PojoHandler.class, REQUEST).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            aws.handleRequest(PojoHandler.class, new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8))).getText()
        );

        assertEquals(
            RESPONSE_TEXT,
            aws.apply(FunctionHandler.class, REQUEST).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            aws.apply(FunctionHandler.class, new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8))).getText()
        );

        assertEquals(
            RESPONSE_TEXT,
            aws.<Response>invoke(PojoHandler.class.getName(), REQUEST).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            aws.<Response>invoke(PojoHandler.class.getName(), new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8))).getText()
        );

        assertEquals(
            RESPONSE_TEXT,
            aws.<Response>invoke(SimpleHandler.class.getName() + "::execute", REQUEST).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            aws.<Response>invoke(SimpleHandler.class.getName() + "::execute", REQUEST).getText()
        );

        assertEquals(
            RESPONSE_TEXT,
            mapper.readValue(aws.stream(PojoStreamHandler.class, REQUEST).toString(), Response.class).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            mapper.readValue(aws.stream(PojoStreamHandler.class, new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8))).toString(), Response.class).getText()
        );

        assertEquals(
            RESPONSE_TEXT,
            mapper.readValue(aws.stream(PojoStreamHandler.class.getName(), REQUEST).toString(), Response.class).getText()
        );
        assertEquals(
            RESPONSE_TEXT,
            mapper.readValue(aws.stream(PojoStreamHandler.class.getName(), new ByteArrayInputStream(REQUEST.getBytes(StandardCharsets.UTF_8))).toString(), Response.class).getText()
        );

    }

}
