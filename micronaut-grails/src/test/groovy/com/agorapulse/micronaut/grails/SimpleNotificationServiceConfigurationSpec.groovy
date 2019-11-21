package com.agorapulse.micronaut.grails

import com.agorapulse.micronaut.aws.sns.SimpleNotificationService
import com.agorapulse.micronaut.aws.sns.SimpleNotificationServiceConfiguration
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

/**
 * Tests for micronaut Spring bean processor.
 */
@ContextConfiguration(classes = [GrailsSimpleNotificationServiceConfig, MicronautGrailsConfiguration])
@TestPropertySource('classpath:com/agorapulse/micronaut/grails/SimpleNotificationServiceConfigurationSpec.properties')
class SimpleNotificationServiceConfigurationSpec extends Specification {

    private static final String ARN = 'arn::dummy'

    @Autowired
    ApplicationContext ctx

    void 'test sns configuration'() {
        expect:
            ctx.containsBean('simpleNotificationServiceConfiguration')
        when:
            SimpleNotificationServiceConfiguration configuration = ctx.getBean(SimpleNotificationServiceConfiguration)
        then:
            configuration
            configuration.android
            configuration.android.arn == ARN
    }
}

@CompileStatic
@Configuration
class GrailsSimpleNotificationServiceConfig {

    @Bean
    MicronautBeanImporter myImporter() {
        MicronautBeanImporter.create()
            .customize(PropertyTranslatingCustomizer
                .builder()
                .replacePrefix('aws.sns', 'grails.plugin.awssdk.sns')
            )
            .addByType(SimpleNotificationService)
            .addByType(SimpleNotificationServiceConfiguration)
    }

}
