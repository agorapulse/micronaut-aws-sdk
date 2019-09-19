package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic

import javax.validation.constraints.NotEmpty

/**
 * Default simple queue service configuration.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    static class Application {
        @NotEmpty String arn
    }

    String topic = ''

    Application ios = new Application()
    Application android = new Application()
    Application amazon = new Application()

}
