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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.function.executor.FunctionInitializer;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PojoStreamHandler extends FunctionInitializer implements RequestStreamHandler {

    @Inject private SomeService service;
    @Inject private ObjectMapper mapper;

    public PojoStreamHandler() { }

    public PojoStreamHandler(ApplicationContext applicationContext) {
        super(applicationContext);
    }


    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        Request request = mapper.readValue(input, Request.class);
        Response response = new Response(service.transform(request.getMessage()));
        mapper.writeValue(output, response);
    }
}
