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
package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.KinesisClient;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.SequenceNumber;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.Stream;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Singleton
@InterceptorBean(KinesisClient.class)
@Requires(classes = software.amazon.awssdk.services.kinesis.KinesisClient.class)
public class KinesisClientIntroduction implements MethodInterceptor<Object, Object> {

    private static final String KEY = "key";
    private static final String SEQUENCE = "sequence";

    private static class RecordArguments {
        Argument<?> partitionKey;
        Argument<?> data;
        Argument<?> sequenceNumber;

        boolean isValid() {
            return partitionKey != null && data != null;
        }

    }

    private final BeanContext beanContext;
    private final ObjectMapper objectMapper;

    public KinesisClientIntroduction(BeanContext beanContext, ObjectMapper objectMapper) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<KinesisClient> clientAnnotationValue = context.getAnnotation(KinesisClient.class);

        if (clientAnnotationValue == null) {
            throw new IllegalStateException("Invocation beanContext is missing required annotation KinesisClient");
        }

        String configurationName = clientAnnotationValue.getValue(String.class).orElse(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME);
        KinesisService service = beanContext.getBean(
            KinesisService.class,
            ConfigurationUtil.isDefaultConfigurationName(configurationName) ? null : Qualifiers.byName(configurationName)
        );

        String streamName = clientAnnotationValue.get(KinesisClient.Constants.STREAM, String.class).orElse(null);

        AnnotationValue<Stream> streamAnnotationValue = context.getAnnotation(Stream.class);
        if (streamAnnotationValue != null) {
            streamName = streamAnnotationValue.getRequiredValue(String.class);
        }

        if (streamName == null || StringUtils.isEmpty(streamName)) {
            streamName = service.getDefaultStreamName();
        }

        try {
            return doIntercept(context, service, streamName);
        } catch (ResourceNotFoundException ignored) {
            service.createStream(streamName);
            return doIntercept(context, service, streamName);
        }
    }

    @SuppressWarnings("unchecked")
    private Object doIntercept(MethodInvocationContext<Object, Object> context, KinesisService service, String streamName) {
        if (context.getArguments().length == 1) {
            Argument<?> arg = context.getArguments()[0];
            Class<?> argType = arg.getType();
            Object param = context.getParameters().get(arg.getName()).getValue();

            if (Event.class.isAssignableFrom(argType)) {
                return service.putEvent(streamName, (Event) param);
            }

            if (PutRecordsRequestEntry.class.isAssignableFrom(argType)) {
                return service.putRecords(streamName, Collections.singletonList((PutRecordsRequestEntry) param));
            }

            if (Iterable.class.isAssignableFrom(argType) && arg.hasTypeVariables() && arg.getFirstTypeVariable().isPresent()) {
                Class<?> iterableType = arg.getFirstTypeVariable().get().getType();

                if (Event.class.isAssignableFrom(iterableType)) {
                    return service.putEvents(streamName, toList((Iterable<Event>) param));
                }

                if (PutRecordsRequestEntry.class.isAssignableFrom(iterableType)) {
                    return service.putRecords(streamName, toList((Iterable<PutRecordsRequestEntry>) param));
                }

                return service.putRecords(streamName, toJsonPutRequests(param));
            }

            if (argType.isArray()) {
                if (Event.class.isAssignableFrom(argType.getComponentType())) {
                    return service.putEvents(streamName, toList(Arrays.asList((Event[]) param)));
                }
                if (PutRecordsRequestEntry.class.isAssignableFrom(argType.getComponentType())) {
                    return service.putRecords(streamName, toList(Arrays.asList((PutRecordsRequestEntry[]) param)));
                }
                if (byte.class.equals(argType.getComponentType())) {
                    return service.putRecord(streamName, createDefaultParititonKey(), (byte[]) param);
                }
                return service.putRecords(streamName, toJsonPutRequests(param));
            }

            if (CharSequence.class.isAssignableFrom(argType)) {
                return service.putRecord(streamName, createDefaultParititonKey(), ((CharSequence) param).toString());
            }

            return sendJson(service, streamName, createDefaultParititonKey(), param, null);
        }

        if (context.getArguments().length == 2 || context.getArguments().length == 3) {
            RecordArguments recordArguments = findArguments(context.getArguments());

            String partitionKey = String.valueOf(context.getParameters().get(recordArguments.partitionKey.getName()).getValue());
            String sequenceNumber = recordArguments.sequenceNumber == null ? null : String.valueOf(context.getParameters().get(recordArguments.sequenceNumber.getName()).getValue());
            Object data = context.getParameters().get(recordArguments.data.getName()).getValue();
            Class<?> dataType = recordArguments.data.getType();

            if (String.class.isAssignableFrom(dataType)) {
                return service.putRecord(streamName, partitionKey, (String) data, sequenceNumber);
            }

            if (dataType.isArray() && byte.class.equals(dataType.getComponentType())) {
                return service.putRecord(streamName, partitionKey, (byte[]) data, sequenceNumber);
            }

            return sendJson(service, streamName, partitionKey, data, sequenceNumber);
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private List<PutRecordsRequestEntry> toJsonPutRequests(Object param) {
        Iterable<?> objects = param instanceof Iterable ? (Iterable<?>) param : Arrays.asList((Object[]) param);
        List<PutRecordsRequestEntry> ret = new ArrayList<>();
        for (Object o : objects) {
            ret.add(PutRecordsRequestEntry.builder().data(SdkBytes.fromByteArray(json(o))).partitionKey(createDefaultParititonKey()).build());
        }
        return ret;
    }

    private byte[] json(Object data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to marshal " + data + " to JSON", e);
        }
    }

    private PutRecordResponse sendJson(KinesisService service, String streamName, String partitionKey, Object data, String sequenceNumber) {
        return service.putRecord(streamName, partitionKey, json(data), sequenceNumber);
    }

    private String createDefaultParititonKey() {
        return UUID.randomUUID().toString();
    }

    private RecordArguments findArguments(Argument<?>[] arguments) {
        RecordArguments names = new RecordArguments();
        for(Argument<?> argument : arguments) {
            if (argument.isAnnotationPresent(PartitionKey.class) || argument.getName().toLowerCase().contains(KEY)) {
                names.partitionKey = argument;
                continue;
            }
            if (argument.isAnnotationPresent(SequenceNumber.class) || argument.getName().toLowerCase().contains(SEQUENCE)) {
                names.sequenceNumber = argument;
                continue;
            }
            names.data = argument;
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument annotated with @PartitionKey and one without any annotation or it needs to have single argument implementing Event");
        }

        return names;
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable instanceof List) {
            return (List<T>) iterable;
        }

        List<T> ret = new ArrayList<>();
        iterable.forEach(ret::add);
        return ret;
    }
}
