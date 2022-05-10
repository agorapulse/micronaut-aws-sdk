package com.agorapulse.micronaut.aws.dynamodb.annotation.remap;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ServiceAnnotationRemapper implements AnnotationRemapper {

    @Nonnull
    @Override
    public String getPackageName() {
        return "com.agorapulse.micronaut.aws.dynamodb.annotation";
    }

    @Nonnull
    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(annotation);
    }

}
