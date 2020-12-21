package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.sqs.SqsClient;

import javax.inject.Named;

/**
 * Default configuration for Simple Queue Service.
 */
@Primary
@Named(ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
@ConfigurationProperties("aws.sqs")
@Requires(classes = SqsClient.class)
public class DefaultSimpleQueueServiceConfiguration extends SimpleQueueServiceConfiguration {

}
