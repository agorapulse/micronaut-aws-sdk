package com.agorapulse.micronaut.amazon.awssdk.s3;

import io.micronaut.context.annotation.ConfigurationProperties;

import javax.inject.Named;

/**
 * Default simple storage service configuration.
 */
@Named("default")
@ConfigurationProperties("aws.s3")
public class DefaultSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

}
