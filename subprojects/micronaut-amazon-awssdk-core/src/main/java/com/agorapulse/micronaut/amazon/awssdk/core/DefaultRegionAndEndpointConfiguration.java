package com.agorapulse.micronaut.amazon.awssdk.core;

import javax.annotation.Nullable;

/**
 * Default region and endpoint configuration.
 */
public class DefaultRegionAndEndpointConfiguration implements RegionAndEndpointConfiguration {

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Nullable private String region;
    @Nullable private String endpoint;
}
