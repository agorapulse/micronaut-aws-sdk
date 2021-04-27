package com.agorapulse.micronaut.amazon.awssdk.cloudwatch;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import javax.inject.Named;

/**
 * Default simple storage service configuration.
 */
@Primary
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@ConfigurationProperties("aws.cloudwatch")
@Requires(classes = CloudWatchClient.class)
public class CloudWatchConfiguration extends DefaultRegionAndEndpointConfiguration {

}
