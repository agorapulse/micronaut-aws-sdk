package com.agorapulse.micronaut.aws.sqs

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests for simple queue service configuration.
 */
class SimpleQueueServiceConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'one present by default with empty queue'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 1
            context.getBean(SimpleQueueServiceConfiguration).queue == ''
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.sqs.queue': 'DefaultQueue'
            )
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 1
            context.getBean(SimpleQueueServiceConfiguration).queue == 'DefaultQueue'
            context.getBean(SimpleQueueService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.sqs.queues.samplequeue.queue': 'SampleQueue'
            )
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 2
            context.getBean(SimpleQueueService)
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue'))
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.sqs.queue': 'DefaultQueue',
                'aws.sqs.queues.samplequeue.queue': 'SampleQueue'
            )
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 2
            context.getBean(SimpleQueueService, Qualifiers.byName('default'))
            context.getBean(SimpleQueueService, Qualifiers.byName('default')).defaultQueueName == 'DefaultQueue'
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue'))
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue')).defaultQueueName == 'SampleQueue'
    }
}
