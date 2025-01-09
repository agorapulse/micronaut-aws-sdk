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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import com.agorapulse.micronaut.amazon.awssdk.kinesis.Event;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.worker.annotation.KinesisListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Singleton
@Requires(property = "aws.kinesis")
@Requires(property = "aws.kinesis.listener.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class KinesisListenerMethodProcessor implements ExecutableMethodProcessor<KinesisListener>, ApplicationEventListener<ShutdownEvent>, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisListenerMethodProcessor.class);

    private static class StringListener implements BiConsumer<String, KinesisClientRecord> {

        private final ExecutableMethod method;
        private final Object bean;

        public StringListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, KinesisClientRecord record) {
            method.invoke(bean, s);
        }
    }

    private static class RecordListener implements BiConsumer<String, KinesisClientRecord> {

        private final ExecutableMethod method;
        private final Object bean;

        RecordListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, KinesisClientRecord record) {
            method.invoke(bean, record);
        }
    }

    private static class StringAndRecordListener implements BiConsumer<String, KinesisClientRecord> {

        private final ExecutableMethod method;
        private final Object bean;

        StringAndRecordListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, KinesisClientRecord record) {
            method.invoke(bean, s, record);
        }
    }

    private static class EventListener implements BiConsumer<String, KinesisClientRecord> {

        private final ExecutableMethod method;
        private final Object bean;
        private final ObjectMapper mapper;

        private final String consumerFilterKey;

        EventListener(ExecutableMethod method, Object bean, ObjectMapper mapper, String consumerFilterKey) {
            this.method = method;
            this.bean = bean;
            this.mapper = mapper;
            this.consumerFilterKey = consumerFilterKey;
        }

        @Override
        public void accept(String s, KinesisClientRecord record) {
            Class type = method.getArguments()[0].getType();
            try {
                Object event = mapper.readValue(s, type);

                if (event instanceof Event && StringUtils.isNotEmpty(consumerFilterKey)) {
                    String eventKey = ((Event) event).getConsumerFilterKey();
                    if (!consumerFilterKey.equals(eventKey)) {
                        LOGGER.debug("Ignoring event because expected consumer filter key {} is not equal to the event's filter key {}: {}", consumerFilterKey, eventKey, s);
                        return;
                    }
                }

                method.invoke(bean, event);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to unmarshall string " + s + " as type " + type);
            }
        }
    }

    private static class EventAndRecordListener implements BiConsumer<String, KinesisClientRecord> {

        private final ExecutableMethod method;
        private final Object bean;
        private final ObjectMapper mapper;

        EventAndRecordListener(ExecutableMethod method, Object bean, ObjectMapper mapper) {
            this.method = method;
            this.bean = bean;
            this.mapper = mapper;
        }

        @Override
        public void accept(String s, KinesisClientRecord record) {
            Class type = method.getArguments()[0].getType();
            try {
                method.invoke(bean, mapper.readValue(s, type), record);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to unmarshall string " + s + " as type " + type);
            }
        }
    }

    private final BeanContext beanContext;
    private final ObjectMapper objectMapper;
    private final KinesisWorkerFactory kinesisWorkerFactory;

    private final String consumerFilterKey;

    private final ConcurrentHashMap<String, KinesisWorker> workers = new ConcurrentHashMap<>();

    public KinesisListenerMethodProcessor(
        BeanContext beanContext,
        ObjectMapper objectMapper,
        KinesisWorkerFactory kinesisWorkerFactory,
        @Value("${aws.kinesis.consumer-filter-key:}") String consumerFilterKey
    ) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
        this.kinesisWorkerFactory = kinesisWorkerFactory;
        this.consumerFilterKey = consumerFilterKey;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        Argument[] arguments = method.getArguments();

        if (arguments.length > 2) {
            throw new IllegalArgumentException("Method must implement at most two arguments");
        }

        if (arguments.length < 1) {
            throw new IllegalArgumentException("Method must implement at least one arguments");
        }

        if (arguments.length == 2 && !KinesisClientRecord.class.isAssignableFrom(arguments[1].getType())) {
            throw new IllegalArgumentException("Second argument must be Record");
        }

        io.micronaut.context.Qualifier<Object> qualifer = beanDefinition
            .getAnnotationTypeByStereotype(Qualifier.class)
            .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
            .orElse(null);

        Class beanType = beanDefinition.getBeanType();
        Object bean = beanContext.getBean(beanType, qualifer);

        BiConsumer<String, KinesisClientRecord> consumer = createConsumer(method, bean);

        String configurationName = method.getValue(KinesisListener.class, String.class).get();

        KinesisWorker worker = workers.computeIfAbsent(
            configurationName, key -> {
                KinesisWorker w = kinesisWorkerFactory.create(getKinesisConfiguration(key));

                LOGGER.debug("Kinesis worker for configuration {} created", key);

                w.start();

                return w;
            }
        );

        worker.addConsumer(consumer);

        LOGGER.debug("Kinesis listener for method {} declared in {} registered", method, beanDefinition.getBeanType());
    }

    @Override
    public void onApplicationEvent(ShutdownEvent event) {
        close();
    }

    @Override
    public void close() {
        workers.values().forEach(KinesisWorker::shutdown);
    }

    private KinesisClientConfiguration getKinesisConfiguration(String key) {
        try {
            return beanContext.getBean(KinesisClientConfiguration.class, Qualifiers.byName(key));
        } catch (NoSuchBeanException ignored) {
            LOGGER.error("Cannot setup listener Kinesis listener, application name is missing. Configuration for Kinesis client with name '{}' is missing", key);
            return null;
        }
    }

    private BiConsumer<String, KinesisClientRecord> createConsumer(ExecutableMethod method, Object bean) {
        Argument[] arguments = method.getArguments();

        if (arguments.length == 2) {
            if (CharSequence.class.isAssignableFrom(arguments[0].getType())) {
                return new StringAndRecordListener(method, bean);
            }
            return new EventAndRecordListener(method, bean, objectMapper);
        }

        if (CharSequence.class.isAssignableFrom(arguments[0].getType())) {
            return new StringListener(method, bean);
        }

        if (KinesisClientRecord.class.isAssignableFrom(arguments[0].getType())) {
            return new RecordListener(method, bean);
        }

        return new EventListener(method, bean, objectMapper, consumerFilterKey);
    }

}
