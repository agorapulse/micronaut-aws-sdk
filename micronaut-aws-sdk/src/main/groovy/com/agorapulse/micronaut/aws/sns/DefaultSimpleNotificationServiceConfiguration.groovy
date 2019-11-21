package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment

import javax.inject.Named

/**
 * Default simple queue service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sns')
@Requires(classes = AmazonSNS)
class DefaultSimpleNotificationServiceConfiguration extends SimpleNotificationServiceConfiguration {

    DefaultSimpleNotificationServiceConfiguration(Environment environment) {
        super('aws.sns', environment)
    }

}
