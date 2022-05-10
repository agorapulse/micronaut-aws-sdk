package com.agorapulse.micronaut.amazon.awssdk.sns.annotation.remap;

import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.NotificationClient;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class NotificationClientRemapper implements AnnotationRemapper {

    @Nonnull
    @Override
    public String getPackageName() {
        return NotificationClient.class.getPackage().getName();
    }

    @Nonnull
    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        return Collections.singletonList(annotation);
    }

}
