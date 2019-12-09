package com.agorapulse.micronaut.aws.s3

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic

import javax.validation.constraints.NotEmpty

/**
 * Default simple storage service configuration.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleStorageServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    @NotEmpty String bucket = ''

}
