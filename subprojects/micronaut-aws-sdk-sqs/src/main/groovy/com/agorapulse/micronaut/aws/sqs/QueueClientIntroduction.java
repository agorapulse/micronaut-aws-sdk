/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.aws.sqs;

import com.agorapulse.micronaut.aws.sqs.annotation.Queue;
import com.agorapulse.micronaut.aws.sqs.annotation.QueueClient;
import com.agorapulse.micronaut.aws.util.ConfigurationUtil;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Singleton
@InterceptorBean(QueueClient.class)
@Requires(classes = AmazonSQS.class)
public class QueueClientIntroduction implements MethodInterceptor<Object, Object> {

    private static final String GROUP = "group";
    private static final String DELAY = "delay";

    private static final Function<String, Optional<String>> EMPTY_IF_UNDEFINED = (String s) -> StringUtils.isEmpty(s) ? Optional.empty() : Optional.of(s);
    private static final Function<Integer, Optional<Integer>> EMPTY_IF_ZERO = (Integer i) -> i == 0 ? Optional.empty() : Optional.of(i);

    private static class QueueArguments {
        Argument<?> message;
        Argument<?> delay;
        Argument<?> group;

        boolean isValid() {
            return message != null;
        }

    }

    private final BeanContext beanContext;
    private final ObjectMapper objectMapper;

    public QueueClientIntroduction(BeanContext beanContext, ObjectMapper objectMapper) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<QueueClient> clientAnnotationValue = context.getAnnotation(QueueClient.class);

        if (clientAnnotationValue == null) {
            throw new IllegalStateException("Invocation beanContext is missing required annotation QueueClient");
        }

        String configurationName = clientAnnotationValue.getValue(String.class).orElse(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME);
        SimpleQueueService service = beanContext.getBean(
            SimpleQueueService.class,
            ConfigurationUtil.isDefaultConfigurationName(configurationName) ? null : Qualifiers.byName(configurationName)
        );

        String queueName = clientAnnotationValue.get(QueueClient.Constants.QUEUE, String.class).flatMap(EMPTY_IF_UNDEFINED).orElse(null);
        String group = clientAnnotationValue.get(QueueClient.Constants.GROUP, String.class).flatMap(EMPTY_IF_UNDEFINED).orElse(null);
        Integer delay = clientAnnotationValue.get(QueueClient.Constants.DELAY, Integer.class).flatMap(EMPTY_IF_ZERO).orElse(0);

        AnnotationValue<Queue> queueAnnotationValue = context.getAnnotation(Queue.class);

        if (queueAnnotationValue != null) {
            queueName = queueAnnotationValue.getRequiredValue(String.class);
            group = queueAnnotationValue.get(QueueClient.Constants.GROUP, String.class).flatMap(EMPTY_IF_UNDEFINED).orElse(group);
            delay = queueAnnotationValue.get(QueueClient.Constants.DELAY, Integer.class).flatMap(EMPTY_IF_ZERO).orElse(delay);
        }

        if (queueName == null) {
            queueName = service.getDefaultQueueName();
        }

        try {
            return doIntercept(context, service, queueName, group, delay);
        } catch (QueueDoesNotExistException ignored) {
            service.createQueue(queueName);
            return doIntercept(context, service, queueName, group, delay);
        }
    }

    private Object doIntercept(MethodInvocationContext<Object, Object> context, SimpleQueueService service, String queueName, String group, Integer delay) {
        Argument[] arguments = context.getArguments();
        Map<String, Object> params = context.getParameterValueMap();

        if (arguments.length == 1 && context.getMethodName().startsWith("delete")) {
            service.deleteMessage(queueName, String.valueOf(params.get(arguments[0].getName())));
            return null;
        }

        if (arguments.length >= 1 && arguments.length <= 3) {
            QueueArguments queueArguments = findArguments(arguments);


            if (queueArguments.delay != null) {
                Object delayParameter = params.get(queueArguments.delay.getName());
                delay = ((Number)delayParameter).intValue();
            }

            if (queueArguments.group != null) {
                Object groupParameter = params.get(queueArguments.group.getName());
                group = String.valueOf(groupParameter);
            }

            Object message = params.get(queueArguments.message.getName());
            Class<?> messageType = queueArguments.message.getType();

            if (CharSequence.class.isAssignableFrom(messageType)) {
                return service.sendMessage(queueName, message.toString(), delay, group);
            }

            if (messageType.isArray() && byte.class.equals(messageType.getComponentType())) {
                return service.sendMessage(queueName, new String((byte[]) message), delay, group);
            }

            return sendJson(service, queueName, message, delay, group);
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private String sendJson(SimpleQueueService service, String queueName, Object message, int delay, String group) {
        try {
            return service.sendMessage(queueName, objectMapper.writeValueAsString(message), delay, group);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to marshal " + message + " to JSON", e);
        }
    }

    private QueueArguments findArguments(Argument[] arguments) {
        QueueArguments names = new QueueArguments();

        for (Argument<?> argument : arguments) {
            if (argument.getName().toLowerCase().contains(GROUP)) {
                names.group = argument;
                continue;
            }
            if (argument.getName().toLowerCase().contains(DELAY) || Number.class.isAssignableFrom(argument.getType())) {
                names.delay = argument;
                continue;
            }
            names.message = argument;
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument which name does not contain group or delay");
        }

        return names;
    }
}
