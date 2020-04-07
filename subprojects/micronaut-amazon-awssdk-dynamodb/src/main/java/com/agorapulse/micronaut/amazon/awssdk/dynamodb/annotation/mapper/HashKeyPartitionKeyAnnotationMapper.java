package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.mapper;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class HashKeyPartitionKeyAnnotationMapper implements NamedAnnotationMapper {


    @Nonnull
    @Override
    public String getName() {
        return "com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.HashKey";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<PartitionKey> builder = AnnotationValue.builder(PartitionKey.class);
        return Collections.singletonList(
            builder.build()
        );
    }

}
