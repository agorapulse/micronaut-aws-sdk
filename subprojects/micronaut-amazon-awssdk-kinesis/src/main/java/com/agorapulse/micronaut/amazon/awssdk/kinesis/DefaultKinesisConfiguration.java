package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.kinesis.KinesisClient;

import javax.inject.Named;

/**
 * Default Kinesis configuration, published with <code>default</code> named qualifier.
 */
@Primary
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@ConfigurationProperties("aws.kinesis")
@Requires(classes = KinesisClient.class)
public class DefaultKinesisConfiguration extends KinesisConfiguration {
}
