package com.agorapulse.micronaut.aws.sqs

import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.validation.constraints.Max
import javax.validation.constraints.Min

@CompileStatic
@ConfigurationProperties("aws.sqs")
@Requires(classes = AmazonSQS.class)
class SimpleQueueServiceConfiguration {

    String queue = ""
    String queueNamePrefix = ""
    boolean autoCreateQueue = false
    boolean cache = false

    @Min(0L) @Max(900L)
    Integer delaySeconds = 0

    @Min(60L) @Max(1209600L)
    Integer messageRetentionPeriod = 345600

    @Min(1024L) @Max(262144L)
    Integer maximumMessageSize = 262144

    @Min(0L) @Max(43200L)
    Integer visibilityTimeout = 30

}
