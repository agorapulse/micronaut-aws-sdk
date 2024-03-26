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

import io.micronaut.context.ApplicationContext;
import io.micronaut.function.aws.MicronautRequestHandler;
import javax.inject.Inject;

public class PojoHandler extends MicronautRequestHandler<Request, Response> {

    @Inject private SomeService service;

    public PojoHandler() { }

    public PojoHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Response handleRequest(Request input) {
        return new Response(service.transform(input.getMessage()));
    }

    @Override
    public Response execute(Request input) {
        return new Response(service.transform(input.getMessage()));
    }

}
