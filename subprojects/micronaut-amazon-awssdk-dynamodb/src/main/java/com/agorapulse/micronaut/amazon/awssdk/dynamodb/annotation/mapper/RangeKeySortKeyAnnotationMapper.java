package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.mapper;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class RangeKeySortKeyAnnotationMapper implements NamedAnnotationMapper {


    @Nonnull
    @Override
    public String getName() {
        return "com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.RangeKey";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        final AnnotationValueBuilder<SortKey> builder = AnnotationValue.builder(SortKey.class);
        return Collections.singletonList(
            builder.build()
        );
    }

}
