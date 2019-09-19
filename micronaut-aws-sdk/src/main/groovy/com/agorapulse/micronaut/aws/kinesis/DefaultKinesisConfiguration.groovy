package com.agorapulse.micronaut.aws.kinesis

import com.amazonaws.services.kinesis.AmazonKinesis
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default Kinesis configuration, published with <code>default</code> named qualifier.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.kinesis')
@Requires(classes = AmazonKinesis)
class DefaultKinesisConfiguration extends KinesisConfiguration {

}
