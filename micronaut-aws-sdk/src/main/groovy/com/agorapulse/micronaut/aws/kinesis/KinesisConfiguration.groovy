package com.agorapulse.micronaut.aws.kinesis

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.kinesis.AmazonKinesis
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.NotEmpty

/**
 * Default Kinesis configuration, published with <code>default</code> named qualifier.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.kinesis')
@Requires(classes = AmazonKinesis)
class KinesisConfiguration extends DefaultRegionAndEndpointConfiguration {

    @NotEmpty String stream = ''

    String consumerFilterKey = ''

}
