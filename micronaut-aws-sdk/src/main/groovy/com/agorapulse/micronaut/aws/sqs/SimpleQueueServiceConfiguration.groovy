package com.agorapulse.micronaut.aws.sqs

import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * Defualt configuration for simple queue service.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sqs')
@Requires(classes = AmazonSQS)
class SimpleQueueServiceConfiguration {

    String queue = ''
    String queueNamePrefix = ''
    boolean autoCreateQueue = false
    boolean cache = false
    boolean fifo = false

    @Min(0L) @Max(900L)
    Integer delaySeconds = 0

    @Min(60L) @Max(1209600L)
    Integer messageRetentionPeriod = 345600

    @Min(1024L) @Max(262144L)
    Integer maximumMessageSize = 262144

    @Min(0L) @Max(43200L)
    Integer visibilityTimeout = 30

}
