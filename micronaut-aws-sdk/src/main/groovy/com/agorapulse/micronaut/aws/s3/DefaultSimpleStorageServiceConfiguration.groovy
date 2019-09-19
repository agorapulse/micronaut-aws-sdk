package com.agorapulse.micronaut.aws.s3

import com.amazonaws.services.s3.AmazonS3
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.s3')
@Requires(classes = AmazonS3)
class DefaultSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

}
