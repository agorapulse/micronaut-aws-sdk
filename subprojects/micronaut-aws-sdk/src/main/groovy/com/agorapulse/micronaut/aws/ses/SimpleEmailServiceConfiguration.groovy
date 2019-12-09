package com.agorapulse.micronaut.aws.ses

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.ses')
@Requires(classes = AmazonSimpleEmailService)
class SimpleEmailServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

}
