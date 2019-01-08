package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

/**
 * Named simple queue service configuration for each property key.
 */
@CompileStatic
@EachProperty('aws.sns.topics')
@Requires(classes = AmazonSNS, property =  'aws.sns.topics')
class NamedSimpleQueueServiceConfiguration extends SimpleNotificationServiceConfiguration {

    final String name

    NamedSimpleQueueServiceConfiguration(@Parameter String name) {
        this.name = name
    }
}
