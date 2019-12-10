/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.sns;

import com.agorapulse.micronaut.aws.sns.annotation.NotificationClient;
import com.agorapulse.micronaut.aws.sns.annotation.Topic;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.transform.Undefined;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Singleton
@Requires(classes = AmazonSNS.class)
public class NotificationClientIntroduction implements MethodInterceptor<Object, Object> {

    private static final String SUBJECT = "subject";
    private static final String NUMBER = "number";

    private static final Function<String, Optional<String>> EMPTY_IF_UNDEFINED = (String s) -> Undefined.STRING.equals(s) ? Optional.empty() : Optional.of(s);

    private static class PublishingArguments {
        Argument<?> message;
        Argument<?> subject;

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

        String configurationName = clientAnnotationValue.getValue(String.class).orElse("default");
        SimpleNotificationService service = beanContext.getBean(SimpleNotificationService.class, Qualifiers.byName(configurationName));

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

        if (arguments.length >= 1 && arguments.length <= 2) {
            PublishingArguments publishingArguments = findArguments(arguments);

            String subject = null;

            if (publishingArguments.subject != null) {
                Object subjectValue = params.get(publishingArguments.subject.getName());
                subject =  subjectValue == null ? null : String.valueOf(subjectValue);
            }

            Object message = params.get(publishingArguments.message.getName());
            Class<?> messageType = publishingArguments.message.getType();

            if (CharSequence.class.isAssignableFrom(messageType)) {
                return service.publishMessageToTopic(topicName, subject, message.toString());
            }

            return publishJson(service, topicName, subject, message);
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private String publishJson(SimpleNotificationService service, String topic, String subject, Object message) {
        try {
            return service.publishMessageToTopic(topic, subject, objectMapper.writeValueAsString(message));
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
