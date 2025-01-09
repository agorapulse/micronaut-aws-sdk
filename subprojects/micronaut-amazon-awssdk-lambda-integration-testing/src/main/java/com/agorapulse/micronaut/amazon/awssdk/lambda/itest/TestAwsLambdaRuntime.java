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

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.api.client.HandlerInfo;
import com.amazonaws.services.lambda.runtime.api.client.LambdaRequestHandler;
import com.amazonaws.services.lambda.runtime.api.client.UserFault;
import com.amazonaws.services.lambda.runtime.api.client.runtimeapi.dto.InvocationRequest;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

@Singleton
public class TestAwsLambdaRuntime {

    private final ApplicationContext context;

    public TestAwsLambdaRuntime(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Creates a request from the given input.
     * @param input the input
     * @return the request
     */
    public static InvocationRequest makeStringRequest(String input) {
        InvocationRequest request = new InvocationRequest();
        request.setContent(input.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /**
     * Creates a request from the given input stream.
     * @param input the input stream
     * @return the request
     * @throws IOException if the input stream cannot be read
     */
    public static InvocationRequest makeStreamRequest(InputStream input) throws IOException {
        InvocationRequest request = new InvocationRequest();
        try (input) {
            request.setContent(input.readAllBytes());
        }
        return request;
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @throws Exception if the invocation fails
     */
    public <O> O invoke(String handler, String input) throws Exception {
        return invoke(handler, makeStringRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @throws Exception if the invocation fails
     */
    public <O> O invoke(String handler, InputStream input) throws Exception {
        return invoke(handler, makeStreamRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param request the request to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @throws Exception if the invocation fails
     */
    @SuppressWarnings("unchecked")
    public <O> O invoke(String handler, InvocationRequest request) throws Exception {
        HandlerInfo handlerInfo = EventHandlerLoader.findHandlerInfo(handler, context);
        LambdaRequestHandler requestHandler = EventHandlerLoader.findRequestHandler(handlerInfo, context);

        if (requestHandler instanceof LambdaRequestHandler.UserFaultHandler) {
            UserFault fault = ((LambdaRequestHandler.UserFaultHandler) requestHandler).fault;
            throw new IllegalArgumentException(fault.reportableError(), fault);
        }


        PojoSerializer<?> serializer = EventHandlerLoader.getSerializerCached(EventHandlerLoader.Platform.UNKNOWN, extractReturnType(handlerInfo));
        ByteArrayOutputStream baos = requestHandler.call(request);
        return (O) serializer.fromJson(baos.toString(StandardCharsets.UTF_8));

    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(Class<? extends RequestStreamHandler> handler, String  input) throws Exception {
        return stream(handler.getName(), makeStringRequest(input));
    }


    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(Class<? extends RequestStreamHandler> handler, InputStream input) throws Exception {
        return stream(handler, makeStreamRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param request the request to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(Class<? extends RequestStreamHandler> handler, InvocationRequest request) throws Exception {
        return stream(handler.getName(), request);
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param input the input to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(String handler, String  input) throws Exception {
        return stream(handler, makeStringRequest(input));
    }


    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param input the input to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(String handler, InputStream input) throws Exception {
        return stream(handler, makeStreamRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the name of the handler with optional method name separated by `::`
     * @param request the request to be passed to the handler
     * @return the result output stream of the invocation
     * @throws Exception if the invocation fails
     */
    public OutputStream stream(String handler, InvocationRequest request) throws Exception {
        HandlerInfo handlerInfo = EventHandlerLoader.findHandlerInfo(handler, context);
        LambdaRequestHandler requestHandler = EventHandlerLoader.findRequestHandler(handlerInfo, context);

        if (requestHandler instanceof LambdaRequestHandler.UserFaultHandler) {
            UserFault fault = ((LambdaRequestHandler.UserFaultHandler) requestHandler).fault;
            throw new IllegalArgumentException(fault.reportableError(), fault);
        }

        return requestHandler.call(request);
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <H> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, H extends RequestHandler<?, O>> O handleRequest(Class<H> handler, String input) throws Exception {
        return handleRequest(handler, makeStringRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <H> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, H extends RequestHandler<?, O>> O handleRequest(Class<H> handler, InputStream input) throws Exception {
        return handleRequest(handler, makeStreamRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param request the request to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <H> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, H extends RequestHandler<?, O>> O handleRequest(Class<H> handler, InvocationRequest request) throws Exception {
        return invoke(handler.getName() + "::handleRequest", request);
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <F> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, F extends Function<?, O>> O apply(Class<F> handler, String input) throws Exception {
        return apply(handler, makeStringRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param input the input to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <F> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, F extends Function<?, O>> O apply(Class<F> handler, InputStream input) throws Exception {
        return apply(handler, makeStreamRequest(input));
    }

    /**
     * Invokes the given handler with the given input.
     * @param handler the handler class
     * @param request the request to be passed to the handler
     * @return the result of the invocation
     * @param <O> the type of the result
     * @param <F> the type of the handler
     * @throws Exception if the invocation fails
     */
    public <O, F extends Function<?, O>> O apply(Class<F> handler, InvocationRequest request) throws Exception {
        return invoke(handler.getName() + "::apply", request);
    }

    private static Type extractReturnType(HandlerInfo handlerInfo) {
        return findMethodConcreteReturnType(handlerInfo.clazz, handlerInfo.methodName == null ? "handleRequest" : handlerInfo.methodName);
    }

    /**
     * Finds the concrete return type of a specified method, including handling for generic superclasses.
     *
     * @param clazz      The class or subclass to inspect.
     * @param methodName The name of the method whose return type you're interested in.
     * @return The concrete Type of the method's return type, considering superclass generic parameters if applicable.
     */
    private static Type findMethodConcreteReturnType(Class<?> clazz, String methodName) {
        Type returnType = null;

        // First, attempt to find the method directly in the class or its superclasses.
        Method method = findMethodInHierarchy(clazz, methodName);
        if (method != null) {
            returnType = method.getGenericReturnType();
        }

        // If the returnType is not a ParameterizedType, attempt to match it against generic superclass parameters.
        if (!(returnType instanceof ParameterizedType)) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                for (Type typeArgument : typeArguments) {
                    if (Objects.requireNonNull(returnType).getTypeName().equals(typeArgument.getTypeName())) {
                        return typeArgument; // Match found in generic superclass parameters.
                    }
                }
            }
        }

        if (returnType == Object.class) {
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                method = findMethodInHierarchy((Class<?>) ((ParameterizedType) genericInterface).getRawType(), methodName);
                if (method != null) {
                    returnType = method.getGenericReturnType();
                    break;
                }
            }
        }

        if (!(returnType instanceof ParameterizedType)) {
            for (Type type : clazz.getGenericInterfaces()) {
                if (type instanceof ParameterizedType parameterizedType) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    Type[] typeArguments = ((Class<?>)parameterizedType.getRawType()).getTypeParameters();
                    for (int i = 0; i < typeArguments.length; i++) {
                        Type typeArgument = typeArguments[i];
                        if (Objects.requireNonNull(returnType).getTypeName().equals(typeArgument.getTypeName())) {
                            return actualTypeArguments[i]; // Match found in generic interface parameters.
                        }
                    }
                }
            }
        }

        return returnType; // Return the found returnType or null if not found.
    }

    private static Method findMethodInHierarchy(Class<?> clazz, String methodName) {
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return method; // Method found in current class or up the hierarchy.
                }
            }
            clazz = clazz.getSuperclass(); // Move up in the class hierarchy.
        }
        return null; // Method not found in class hierarchy.
    }


}
