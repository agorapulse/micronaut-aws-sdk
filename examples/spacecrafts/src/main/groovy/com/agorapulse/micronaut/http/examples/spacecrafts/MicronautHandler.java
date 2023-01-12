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
package com.agorapulse.micronaut.http.examples.spacecrafts;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.function.aws.proxy.MicronautLambdaContainerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class MicronautHandler implements RequestStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicronautHandler.class);

    private static MicronautLambdaContainerHandler handler;
    private static ApplicationContextBuilder builder;

    static {
        reset();
    }

    /**
     * Resets the current handler. For testing purposes only.
     */
    public static void reset() {
        reset(b -> {});
    }

    /**
     * Resets the current handler. For testing purposes only.
     *
     * @param configuration builder customizer
     */
    public static void reset(Consumer<ApplicationContextBuilder> configuration) {
        try {
            builder = ApplicationContext.builder();
            configuration.accept(builder);
            handler = new MicronautLambdaContainerHandler(builder);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Exception in container initialization", e);
            }
            throw new IllegalStateException("Could not initialize Micronaut", e);
        }
    }

    public static ApplicationContext getApplicationContext() {
        if (handler == null) {
            reset();
        }
        return handler.getApplicationContext();
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
