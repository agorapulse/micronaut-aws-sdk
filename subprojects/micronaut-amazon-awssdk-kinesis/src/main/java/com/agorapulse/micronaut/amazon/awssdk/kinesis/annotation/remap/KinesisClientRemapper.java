package com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.remap;

import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.KinesisClient;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class KinesisClientRemapper implements AnnotationRemapper {

    @Nonnull
    @Override
    public String getPackageName() {
        return KinesisClient.class.getPackage().getName();
    }

    @Nonnull
    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(annotation);
    }

}
