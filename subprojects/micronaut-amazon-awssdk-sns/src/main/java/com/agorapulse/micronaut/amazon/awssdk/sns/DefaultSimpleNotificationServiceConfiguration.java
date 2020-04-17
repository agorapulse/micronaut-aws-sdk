package com.agorapulse.micronaut.amazon.awssdk.sns;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.env.Environment;

import javax.inject.Named;

/**
 * Default simple queue service configuration.
 */
@Named("default")
@ConfigurationProperties("aws.sns")
public class DefaultSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {

    public DefaultSimpleNotificationServiceConfiguration(Environment environment) {
        super("aws.sns", environment);
    }

}
