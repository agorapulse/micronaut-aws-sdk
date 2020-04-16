package com.agorapulse.micronaut.amazon.awssdk.s3;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

/**
 * Simple storage service configuration for each configuration key.
 */
@EachProperty("aws.s3.buckets")
public class NamedSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

    public NamedSimpleStorageServiceConfiguration(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private String name;

}
