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
package com.agorapulse.micronaut.amazon.awssdk.lambda;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import com.agorapulse.micronaut.amazon.awssdk.lambda.annotation.Body;
import com.agorapulse.micronaut.amazon.awssdk.lambda.annotation.LambdaClient;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jackson.JacksonConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

@Singleton
@InterceptorBean(LambdaClient.class)
@Requires(classes = software.amazon.awssdk.services.lambda.LambdaClient.class)
public class LambdaClientIntroduction implements MethodInterceptor<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaClientIntroduction.class);

    private static final Function<String, Optional<String>> EMPTY_IF_UNDEFINED = (String s) -> StringUtils.isEmpty(s) ? Optional.empty() : Optional.of(s);

    private final BeanContext beanContext;
    private final ObjectMapper objectMapper;

    public LambdaClientIntroduction(BeanContext beanContext, ObjectMapper objectMapper) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<LambdaClient> clientAnnotationValue = context.getAnnotation(LambdaClient.class);

        if (clientAnnotationValue == null) {
            throw new IllegalStateException("Invocation beanContext is missing required annotation QueueClient");
        }

        String configurationName = clientAnnotationValue.getValue(String.class).orElse(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME);

        software.amazon.awssdk.services.lambda.LambdaClient service = beanContext.getBean(
            software.amazon.awssdk.services.lambda.LambdaClient.class,
            ConfigurationUtil.isDefaultConfigurationName(configurationName) ? null : Qualifiers.byName(configurationName)
        );

        LambdaConfiguration configuration = beanContext.getBean(
            LambdaConfiguration.class,
            ConfigurationUtil.isDefaultConfigurationName(configurationName) ? null : Qualifiers.byName(configurationName)
        );

        String functionName = clientAnnotationValue.get(LambdaClient.Constants.FUNCTION, String.class).flatMap(EMPTY_IF_UNDEFINED).orElse(null);


        if (functionName == null) {
            functionName = configuration.getFunction();
        }

        return doIntercept(context, service, functionName, configuration.getTimeout());
    }

    @SuppressWarnings("rawtypes")
    private Object doIntercept(MethodInvocationContext<Object, Object> context, software.amazon.awssdk.services.lambda.LambdaClient service, String functionName, Duration timeout) {
        Argument[] arguments = context.getArguments();

        if (arguments.length == 1 && context.getArguments()[0].isAnnotationPresent(Body.class)) {
            return invokeFunction(context, service, functionName, timeout, context.getParameterValues()[0]);
        }

        return invokeFunction(context, service, functionName, timeout, context.getParameterValueMap());
    }

    private Object invokeFunction(MethodInvocationContext<Object, Object> context, software.amazon.awssdk.services.lambda.LambdaClient service, String functionName, Duration timeout, Object requestObject) {
        try {
            boolean event = void.class.equals(context.getReturnType().getType());
            String payload = objectMapper.writeValueAsString(requestObject);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Invoking function {} with a payload {}", functionName, payload);
            }

            InvokeRequest request = InvokeRequest
                .builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .invocationType(event ? InvocationType.EVENT : InvocationType.REQUEST_RESPONSE)
                .overrideConfiguration(r -> r.apiCallTimeout(timeout))
                .build();

            InvokeResponse response = service.invoke(request);

            if (response.statusCode() >= 400 || StringUtils.isNotEmpty(response.functionError())) {
                throw new LambdaClientException(response);
            }


            if (event) {
                return null;
            }

            JavaType javaType = JacksonConfiguration.constructType(context.getReturnType().asArgument(), objectMapper.getTypeFactory());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Response from the function {} is {}", functionName, new String(response.payload().asByteArray()));
            }

            return objectMapper.readValue(response.payload().asByteArray(), javaType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot invoke function " + functionName, e);
        }
    }

}
