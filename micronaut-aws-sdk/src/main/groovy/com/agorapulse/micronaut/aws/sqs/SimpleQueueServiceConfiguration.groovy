package com.agorapulse.micronaut.aws.sqs

import com.agorapulse.micronaut.aws.RegionAndEndpointConfiguration
import groovy.transform.CompileStatic

import javax.annotation.Nullable

/**
 * Default configuration for Simple Queue Service.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleQueueServiceConfiguration extends QueueConfiguration implements RegionAndEndpointConfiguration {

    String queueNamePrefix = ''
    boolean autoCreateQueue = false
    boolean cache = false

    @Nullable String region
    @Nullable String endpoint

}
