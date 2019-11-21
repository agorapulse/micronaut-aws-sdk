package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic
import io.micronaut.context.env.Environment

/**
 * Default simple queue service configuration.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    static class Application {
        final String arn

        Application(String arn) {
            this.arn = arn
        }
    }

    private final Environment environment

    final Application ios
    final Application android
    final Application amazon

    String topic = ''

    protected SimpleNotificationServiceConfiguration(String prefix, Environment environment) {
        this.environment = environment
        ios = forPlatform(prefix, 'ios', environment)
        android = forPlatform(prefix, 'android', environment)
        amazon = forPlatform(prefix, 'amazon', environment)
    }

    @SuppressWarnings('DuplicateStringLiteral')
    private static Application forPlatform(String prefix, String platform, Environment environment) {
        return new Application(
            environment.get(prefix + '.' + platform + '.arn', String).orElseGet {
                environment.get(prefix + '.' + platform + '.applicationArn', String).orElse(null)
            })
    }
}
