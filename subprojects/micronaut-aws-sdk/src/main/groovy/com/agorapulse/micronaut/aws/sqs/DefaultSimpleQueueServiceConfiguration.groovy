package com.agorapulse.micronaut.aws.sqs

import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default configuration for Simple Queue Service.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sqs')
@Requires(classes = AmazonSQS)
class DefaultSimpleQueueServiceConfiguration extends SimpleQueueServiceConfiguration {

}
