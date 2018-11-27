package com.agorapulse.micronaut.aws.kinesis.client;

import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Singleton
public class KinesisListenerMethodProcessor implements ExecutableMethodProcessor<KinesisListener> {

    private static class StringListener implements BiConsumer<String, Record> {

        private final ExecutableMethod method;
        private final Object bean;

        public StringListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, Record record) {
            method.invoke(bean, s);
        }
    }

    private static class RecordListener implements BiConsumer<String, Record> {

        private final ExecutableMethod method;
        private final Object bean;

        public RecordListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, Record record) {
            method.invoke(bean, record);
        }
    }

    private static class StringAndRecordListener implements BiConsumer<String, Record> {

        private final ExecutableMethod method;
        private final Object bean;

        public StringAndRecordListener(ExecutableMethod method, Object bean) {
            this.method = method;
            this.bean = bean;
        }

        @Override
        public void accept(String s, Record record) {
            method.invoke(bean, s, record);
        }
    }

    private static class EventListener implements BiConsumer<String, Record> {

        private final ExecutableMethod method;
        private final Object bean;
        private final ObjectMapper mapper;

        public EventListener(ExecutableMethod method, Object bean, ObjectMapper mapper) {
            this.method = method;
            this.bean = bean;
            this.mapper = mapper;
        }

        @Override
        public void accept(String s, Record record) {
            Class type = method.getArguments()[0].getType();
            try {
                method.invoke(bean, mapper.readValue(s, type));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to unmarshall string " + s + " as type " + type);
            }
        }
    }

    private static class EventAndRecordListener implements BiConsumer<String, Record> {

        private final ExecutableMethod method;
        private final Object bean;
        private final ObjectMapper mapper;

        public EventAndRecordListener(ExecutableMethod method, Object bean, ObjectMapper mapper) {
            this.method = method;
            this.bean = bean;
            this.mapper = mapper;
        }

        @Override
        public void accept(String s, Record record) {
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

    public KinesisListenerMethodProcessor(BeanContext beanContext, ObjectMapper objectMapper) {
        this.beanContext = beanContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        System.out.println("bean: " + beanDefinition);
        System.out.println("method: " + method);

        Argument[] arguments = method.getArguments();

        if (arguments.length > 2) {
            throw new IllegalArgumentException("Method must implement at most two arguments");
        }

        if (arguments.length < 1) {
            throw new IllegalArgumentException("Method must implement at least one arguments");
        }

        if (arguments.length == 2 && !Record.class.isAssignableFrom(arguments[1].getType())) {
            throw new IllegalArgumentException("Second argument must be Record");
        }

        io.micronaut.context.Qualifier<Object> qualifer = beanDefinition
            .getAnnotationTypeByStereotype(Qualifier.class)
            .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
            .orElse(null);

        Class<Object> beanType = (Class<Object>) beanDefinition.getBeanType();
        Object bean = beanContext.getBean(beanType, qualifer);

        ExecutableMethod rawMethod = method;

        createConsumer(method, bean).accept("{\"value\": \"foo\"}", new Record());
    }

    private BiConsumer<String, Record> createConsumer(ExecutableMethod method, Object bean) {
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

        if (Record.class.isAssignableFrom(arguments[0].getType())) {
            return new RecordListener(method, bean);
        }

        return new EventListener(method, bean, objectMapper);
    }

}
