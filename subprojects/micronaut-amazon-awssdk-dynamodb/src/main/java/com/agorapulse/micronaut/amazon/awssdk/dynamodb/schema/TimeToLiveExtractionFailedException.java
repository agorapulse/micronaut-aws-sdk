package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import io.micronaut.core.beans.BeanProperty;

public class TimeToLiveExtractionFailedException extends IllegalStateException {

    private final transient Object instance;
    private final transient BeanProperty<?, ?> property;

    public TimeToLiveExtractionFailedException(Object instance, BeanProperty<?, ?> property, String message, Throwable cause) {
        super(message, cause);
        this.instance = instance;
        this.property = property;
    }

    @SuppressWarnings("java:S1452")
    public BeanProperty<?, ?> getProperty() {
        return property;
    }

    public Object getInstance() {
        return instance;
    }

}
