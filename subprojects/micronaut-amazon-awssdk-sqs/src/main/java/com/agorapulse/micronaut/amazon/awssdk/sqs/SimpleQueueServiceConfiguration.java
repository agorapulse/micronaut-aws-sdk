package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.agorapulse.micronaut.amazon.awssdk.core.RegionAndEndpointConfiguration;

import javax.annotation.Nullable;

/**
 * Default configuration for Simple Queue Service.
 */
public abstract class SimpleQueueServiceConfiguration extends QueueConfiguration implements RegionAndEndpointConfiguration {

    private String queueNamePrefix = "";
    private boolean autoCreateQueue;
    private boolean cache;

    @Nullable private String region;
    @Nullable private String endpoint;

    public String getQueueNamePrefix() {
        return queueNamePrefix;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    public boolean isAutoCreateQueue() {
        return autoCreateQueue;
    }

    public void setAutoCreateQueue(boolean autoCreateQueue) {
        this.autoCreateQueue = autoCreateQueue;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

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

}
