package com.agorapulse.micronaut.aws.cloudwatch

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

import javax.inject.Named

@CompileStatic
@EachProperty('aws.cloudwatch.namespaces')
@Requires(classes = AmazonCloudWatch, property = 'aws.cloudwatch.namespaces')
class NamedCloudWatchConfiguration {

    final String name

    NamedCloudWatchConfiguration(@Parameter String name) {
        this.name = name
    }

}
