package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;

import javax.validation.constraints.NotEmpty;

/**
 * Default Kinesis configuration, published with <code>default</code> named qualifier.
 */
public abstract class KinesisConfiguration extends DefaultRegionAndEndpointConfiguration {

    @NotEmpty
    private String stream = "";
    private String consumerFilterKey = "";

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getConsumerFilterKey() {
        return consumerFilterKey;
    }

    public void setConsumerFilterKey(String consumerFilterKey) {
        this.consumerFilterKey = consumerFilterKey;
    }
}
