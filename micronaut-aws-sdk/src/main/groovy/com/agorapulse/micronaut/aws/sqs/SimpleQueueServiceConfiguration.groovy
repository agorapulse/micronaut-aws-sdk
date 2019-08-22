package com.agorapulse.micronaut.aws.sqs

import com.agorapulse.micronaut.aws.RegionAndEndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.annotation.Nullable
import javax.inject.Named

/**
 * Default configuration for Simple Queue Service.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sqs')
@Requires(classes = AmazonSQS)
class SimpleQueueServiceConfiguration extends QueueConfiguration implements RegionAndEndpointConfiguration {

    String queueNamePrefix = ''
    boolean autoCreateQueue = false
    boolean cache = false

    @Nullable String region
    @Nullable String endpoint

}
