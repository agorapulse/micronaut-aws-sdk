package com.agorapulse.micronaut.aws.sqs

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

class SimpleQueueServiceConfigurationSpec extends Specification {
    @AutoCleanup ApplicationContext context = null

    void 'no service present by default'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 0
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.sqs.queue': 'DefaultQueue'
            )
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 1
            context.getBean(SimpleQueueService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.sqs.queues.samplequeue.queue': 'SampleQueue'
            )
        then:
            context.getBeanDefinitions(SimpleQueueService).size() == 1
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
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue'))
    }
}
