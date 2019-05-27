package com.agorapulse.micronaut.aws.cloudwatch

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

@CompileStatic
@Named('default')
@ConfigurationProperties('aws.cloudwatch')
@Requires(classes = AmazonCloudWatch)
class DefaultCloudWatchConfiguration {

    String namespace

}
