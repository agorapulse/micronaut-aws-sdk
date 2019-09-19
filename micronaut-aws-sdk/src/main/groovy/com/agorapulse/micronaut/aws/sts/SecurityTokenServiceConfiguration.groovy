package com.agorapulse.micronaut.aws.sts

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sts')
@Requires(classes = AWSSecurityTokenService)
class SecurityTokenServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

}
