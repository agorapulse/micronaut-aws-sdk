package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.NotEmpty

/**
 * Default simple queue service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sns')
@Requires(classes = AmazonSNS)
class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    static class Application {
        @NotEmpty String arn
    }

    String topic = ''

    Application ios = new Application()
    Application android = new Application()
    Application amazon = new Application()

}
