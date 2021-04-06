package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.kinesis.KinesisClient;

/**
 * Named Kinesis configuration, published with named qualifier of the same name as is the key of this configuration.
 */
@EachProperty(value = "aws.kinesis.streams", primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@Requires(classes = KinesisClient.class)
public class NamedKinesisConfiguration extends KinesisConfiguration {
    private final String name;

    public NamedKinesisConfiguration(@Parameter String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

}
