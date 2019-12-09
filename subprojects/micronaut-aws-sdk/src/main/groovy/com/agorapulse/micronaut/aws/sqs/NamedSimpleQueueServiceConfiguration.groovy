package com.agorapulse.micronaut.aws.sqs

import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

/**
 * Named configuration for simple queue service created for each key in the configuration.
 */
@CompileStatic
@EachProperty('aws.sqs.queues')
@Requires(classes = AmazonSQS, property =  'aws.sqs.queues')
class NamedSimpleQueueServiceConfiguration extends SimpleQueueServiceConfiguration {

    final String name

    NamedSimpleQueueServiceConfiguration(@Parameter String name) {
        this.name = name
    }

}
