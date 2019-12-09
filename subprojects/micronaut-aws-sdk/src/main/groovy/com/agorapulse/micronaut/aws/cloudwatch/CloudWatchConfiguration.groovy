package com.agorapulse.micronaut.aws.cloudwatch

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.cloudwatch')
@Requires(classes = AmazonCloudWatch)
class CloudWatchConfiguration extends DefaultRegionAndEndpointConfiguration {

}
