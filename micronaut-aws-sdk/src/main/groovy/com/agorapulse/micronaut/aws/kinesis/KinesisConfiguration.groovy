package com.agorapulse.micronaut.aws.kinesis

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic

import javax.validation.constraints.NotEmpty

/**
 * Default Kinesis configuration, published with <code>default</code> named qualifier.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class KinesisConfiguration extends DefaultRegionAndEndpointConfiguration {

    @NotEmpty String stream = ''

    String consumerFilterKey = ''

}
