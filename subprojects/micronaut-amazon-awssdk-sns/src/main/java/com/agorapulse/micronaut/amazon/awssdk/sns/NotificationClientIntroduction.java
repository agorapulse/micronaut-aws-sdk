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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.MessageDeduplicationId;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.MessageGroupId;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.NotificationClient;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Singleton
public class NotificationClientIntroduction implements MethodInterceptor<Object, Object> {

    private static final String SUBJECT = "subject";
    private static final String ATTRIBUTES = "attributes";
    private static final String NUMBER = "number";
    private static final String MESSAGE_GROUP_ID = "messageGroupId";
    private static final String MESSAGE_DEDUPLICATION_ID = "messageDeduplicationId";

    private static final Function<String, Optional<String>> EMPTY_IF_UNDEFINED = (String s) -> StringUtils.isEmpty(s) ? Optional.empty() : Optional.of(s);

    private static class PublishingArguments {
        Argument<?> message;
        Argument<?> subject;
        Argument<?> attributes;
        Argument<?> messageGroupId;
        Argument<?> messageDeduplicationId;
        
        boolean isValid() {
            return message != null;
        }
    }

    private static class SmsMessageArguments {
        Argument<?> message;
        Argument<?> phoneNumber;
        Argument<?> attributes;

        boolean isValid() {
            return message != null && phoneNumber != null;
        }
    }

    private final BeanContext beanContext;
    private final ObjectMapper objectMapper;

    public NotificationClientIntroduction(BeanContext beanContext, ObjectMapper objectMapper) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<NotificationClient> clientAnnotationValue = context.getAnnotation(NotificationClient.class);

        if (clientAnnotationValue == null) {
            throw new IllegalStateException("Invocation beanContext is missing required annotation NotificationClient");
        }

        String configurationName = clientAnnotationValue.getValue(String.class).orElse(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME);
        SimpleNotificationService service = beanContext.getBean(
            SimpleNotificationService.class,
            ConfigurationUtil.isDefaultConfigurationName(configurationName) ? null : Qualifiers.byName(configurationName)
        );

        String topicName = clientAnnotationValue.get(NotificationClient.Constants.TOPIC, String.class).flatMap(EMPTY_IF_UNDEFINED).orElse(null);

        AnnotationValue<Topic> topicAnnotaitonValue = context.getAnnotation(Topic.class);

        if (topicAnnotaitonValue != null) {
            topicName = topicAnnotaitonValue.getRequiredValue(String.class);
        }

        if (topicName == null) {
            topicName = service.getDefaultTopicNameOrArn();
        }

        try {
            return doIntercept(context, service, topicName);
        } catch (NotFoundException nfe) {
            service.createTopic(topicName);
            return doIntercept(context, service, topicName);
        }
    }

    private Object doIntercept(MethodInvocationContext<Object, Object> context, SimpleNotificationService service, String topicName) {
        Argument[] arguments = context.getArguments();
        Map<String, Object> params = context.getParameterValueMap();

        if (context.getMethodName().toLowerCase().contains("sms")) {
            SmsMessageArguments smsMessageArguments = findSmsArguments(arguments);

            String phoneNumber = String.valueOf(params.get(smsMessageArguments.phoneNumber.getName()));
            String message = String.valueOf(params.get(smsMessageArguments.message.getName()));

            Map attributes = Collections.emptyMap();

            if (smsMessageArguments.attributes != null) {
                attributes = (Map) params.get(smsMessageArguments.attributes.getName());
            }

            return service.sendSMSMessage(phoneNumber, message, attributes);
        }

        if (arguments.length >= 1 && arguments.length <= 3) {
            PublishingArguments publishingArguments = findArguments(arguments);

            String subject = null;

            if (publishingArguments.subject != null) {
                Object subjectValue = params.get(publishingArguments.subject.getName());
                subject =  subjectValue == null ? null : String.valueOf(subjectValue);
            }

            Map<String, String> attributes = new HashMap<>();
            if (publishingArguments.attributes != null) {
                Map<String, Object> attrs = (Map<String, Object>) params.get(publishingArguments.attributes.getName());
                attrs.forEach((key, value) -> {
                    if (value != null) {
                        attributes.put(key, value.toString());
                    }
                });
            }

            Object message = params.get(publishingArguments.message.getName());
            Class<?> messageType = publishingArguments.message.getType();

            String preparedMessage = "";
            if (CharSequence.class.isAssignableFrom(messageType)) {
                preparedMessage = message.toString();
            } else {
                preparedMessage = toJsonMessage(message);
            }

            if (SimpleNotificationService.isFifoTopic(topicName)) {
                PublishRequest.Builder publishRequestBuilder = PublishRequest.builder();
                publishRequestBuilder.subject(subject);
                publishRequestBuilder.message(preparedMessage);
                if (publishingArguments.messageGroupId != null) {
                    publishRequestBuilder.messageGroupId((String) params.get(publishingArguments.messageGroupId.getName()));
                }
                if (publishingArguments.messageDeduplicationId != null) {
                    publishRequestBuilder.messageDeduplicationId((String) params.get(publishingArguments.messageDeduplicationId.getName()));
                }
                return service.publishRequest(topicName, attributes, publishRequestBuilder);
            } else {
                return service.publishMessageToTopic(topicName, subject, preparedMessage, attributes);
            }
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private String toJsonMessage(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to marshal " + message + " to JSON", e);
        }
    }

    private PublishingArguments findArguments(Argument[] arguments) {
        PublishingArguments names = new PublishingArguments();

        for (Argument<?> argument : arguments) {
            if (argument.getName().toLowerCase().contains(SUBJECT)) {
                names.subject = argument;
                continue;
            }
            // attributes are map and must contain attributes or must come after the message (as the message can be map as well)
            if (Map.class.isAssignableFrom(argument.getType()) && (argument.getName().toLowerCase().contains(ATTRIBUTES) || names.message != null)) {
                names.attributes = argument;
                continue;
            }
            if (argument.getName().equalsIgnoreCase(MESSAGE_GROUP_ID) || argument.isAnnotationPresent(MessageGroupId.class)) {
                names.messageGroupId = argument;
                continue;
            }
            if (argument.getName().equalsIgnoreCase(MESSAGE_DEDUPLICATION_ID) || argument.isAnnotationPresent(MessageDeduplicationId.class)) {
                names.messageDeduplicationId = argument;
                continue;
            }
            names.message = argument;
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument which name does not contain subject");
        }

        return names;
    }

    private SmsMessageArguments findSmsArguments(Argument[] arguments) {
        SmsMessageArguments names = new SmsMessageArguments();

        for (Argument<?> argument : arguments) {
            if (argument.getName().toLowerCase().contains(NUMBER)) {
                names.phoneNumber = argument;
                continue;
            }
            if (Map.class.isAssignableFrom(argument.getType())) {
                names.attributes = argument;
                continue;
            }
            names.message = argument;
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least two phone number and message");
        }

        return names;
    }

}
