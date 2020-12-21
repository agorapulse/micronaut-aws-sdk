package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Named configuration for simple queue service created for each key in the configuration.
 */
@Requires(classes = SqsClient.class, property = "aws.sqs.queues")
@EachProperty(value = "aws.sqs.queues", primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
public class NamedSimpleQueueServiceConfiguration extends SimpleQueueServiceConfiguration {
    private final String name;

    public NamedSimpleQueueServiceConfiguration(@Parameter String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

}
