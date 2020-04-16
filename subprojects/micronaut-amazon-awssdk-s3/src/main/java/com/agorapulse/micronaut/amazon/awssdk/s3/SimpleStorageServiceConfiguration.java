package com.agorapulse.micronaut.amazon.awssdk.s3;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;

import javax.validation.constraints.NotEmpty;

/**
 * Default simple storage service configuration.
 */
public abstract class SimpleStorageServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @NotEmpty
    private String bucket = "";

}
