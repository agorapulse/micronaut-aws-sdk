package com.agorapulse.micronaut.amazon.awssdk.sns;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.env.Environment;

/**
 * Named simple queue service configuration for each property key.
 */
@EachProperty("aws.sns.topics")
public class NamedSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {
    public NamedSimpleNotificationServiceConfiguration(@Parameter String name, Environment environment) {
        super("aws.sns.topics." + name, environment);
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    private final String name;
}
