package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisClient;
import com.agorapulse.micronaut.aws.kinesis.annotation.PartitionKey;
import com.agorapulse.micronaut.aws.kinesis.annotation.SequenceNumber;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
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

        String configurationName = clientAnnotationValue.getValue(String.class).orElse("default");
        KinesisService service = beanContext.getBean(KinesisService.class, Qualifiers.byName(configurationName));

//        AnnotationValue<Stream> streamAnnotationValue = beanContext.getAnnotation(Stream.class);


        if (context.getArguments().length == 1) {
            Argument arg = context.getArguments()[0];
            Class argType = arg.getType();
            Object param = context.getParameters().get(arg.getName()).getValue();

            if (Event.class.isAssignableFrom(argType)) {
                return service.putEvent((Event) param);
            }

            if (PutRecordsRequestEntry.class.isAssignableFrom(argType)) {
                return service.putRecords(Collections.singletonList((PutRecordsRequestEntry) param));
            }

            if (Iterable.class.isAssignableFrom(argType) && arg.hasTypeVariables() && arg.getFirstTypeVariable().isPresent()) {
                Class iterableType = arg.getFirstTypeVariable().get().getType();

                if (Event.class.isAssignableFrom(iterableType)) {
                    return service.putEvents(toList((Iterable<Event>) param));
                }

                if (PutRecordsRequestEntry.class.isAssignableFrom(iterableType)) {
                    return service.putRecords(toList((Iterable<PutRecordsRequestEntry>) param));
                }

            }

            if (argType.isArray()) {
                if (Event.class.isAssignableFrom(argType.getComponentType())) {
                    return service.putEvents(toList(Arrays.asList((Event[]) param)));
                }
                if (PutRecordsRequestEntry.class.isAssignableFrom(argType.getComponentType())) {
                    return service.putRecords(toList(Arrays.asList((PutRecordsRequestEntry[]) param)));
                }
            }

            throw new UnsupportedOperationException("Method is not implemented for argument " + arg + "! You need to specify partition key. See @PartitionKey");
        }

        if (context.getArguments().length == 2 || context.getArguments().length == 3) {
            RecordArguments recordArguments = findArguments(context.getArguments());

            String partitionKey = String.valueOf(context.getParameters().get(recordArguments.partitionKey.getName()).getValue());
            String sequenceNumber = recordArguments.sequenceNumber == null ? null : String.valueOf(context.getParameters().get(recordArguments.sequenceNumber.getName()).getValue());
            Object data = context.getParameters().get(recordArguments.data.getName()).getValue();
            Class<?> dataType = recordArguments.data.getType();

            if (String.class.isAssignableFrom(dataType)) {
                return service.putRecord(partitionKey, (String) data, sequenceNumber);
            }

            if (dataType.isArray() && byte.class.equals(dataType.getComponentType())) {
                return service.putRecord(partitionKey, (byte[]) data, sequenceNumber);
            }

            try {
                return service.putRecord(partitionKey, objectMapper.writeValueAsBytes(data), sequenceNumber);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to marshal " + data + " to JSON", e);
            }
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private RecordArguments findArguments(Argument[] arguments) {
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
