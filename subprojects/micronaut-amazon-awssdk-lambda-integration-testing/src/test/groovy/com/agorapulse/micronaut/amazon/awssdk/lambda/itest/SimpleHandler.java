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
package com.agorapulse.micronaut.amazon.awssdk.lambda.itest;

import io.micronaut.context.ApplicationContext;
import io.micronaut.function.executor.FunctionInitializer;
import jakarta.inject.Inject;

public class SimpleHandler extends FunctionInitializer {

    @Inject private SomeService service;

    public SimpleHandler() { }

    public SimpleHandler(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public Response execute(Request input) {
        return new Response(service.transform(input.getMessage()));
    }

}
