package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

@CompileStatic
@EachProperty('aws.sns.topics')
@Requires(classes = AmazonSNS.class, property =  'aws.sns.topics')
class NamedSimpleQueueServiceConfiguration extends SimpleNotificationServiceConfiguration {

    final String name

    NamedSimpleQueueServiceConfiguration(@Parameter String name) {
        this.name = name
    }
}
