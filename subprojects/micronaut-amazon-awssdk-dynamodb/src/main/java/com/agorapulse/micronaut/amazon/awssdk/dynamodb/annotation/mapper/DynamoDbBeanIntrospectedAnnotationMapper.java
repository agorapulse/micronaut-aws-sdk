package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.mapper;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class DynamoDbBeanIntrospectedAnnotationMapper implements NamedAnnotationMapper {


    @Nonnull
    @Override
    public String getName() {
        return "software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<Introspected> builder = AnnotationValue.builder(Introspected.class);
        return Collections.singletonList(
            builder.build()
        );
    }
}
