package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Default simple queue service configuration.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    static class Application {
        String arn
    }

    @ConfigurationProperties('ios')
    static class IosApplication extends Application { }

    @ConfigurationProperties('android')
    static class AndroidApplication extends Application { }

    @ConfigurationProperties('amazon')
    static class AmazonApplication extends Application { }

    String topic = ''

    IosApplication ios = new IosApplication()
    AndroidApplication android = new AndroidApplication()
    AmazonApplication amazon = new AmazonApplication()

}
