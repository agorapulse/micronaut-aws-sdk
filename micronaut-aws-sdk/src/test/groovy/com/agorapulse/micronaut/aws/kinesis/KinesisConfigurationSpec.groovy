package com.agorapulse.micronaut.aws.kinesis

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests for Kinesis configuration setup.
 */
class KinesisConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'one service present by default'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(KinesisService).size() == 1
            context.getBean(KinesisConfiguration).stream == ''
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.stream': 'StreamName'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 1
            context.getBean(KinesisConfiguration).stream == 'StreamName'
            context.getBean(KinesisService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.streams.sample.stream': 'SampleStream'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 2
            context.getBean(KinesisService)
            context.getBean(KinesisService, Qualifiers.byName('default'))
            context.getBean(KinesisService, Qualifiers.byName('sample'))
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.kinesis.stream': 'StreamName',
                'aws.kinesis.streams.sample.stream': 'SampleStream'
            )
        then:
            context.getBeanDefinitions(KinesisService).size() == 2
            context.getBean(KinesisService, Qualifiers.byName('default'))
            context.getBean(KinesisService, Qualifiers.byName('sample'))
    }

}
