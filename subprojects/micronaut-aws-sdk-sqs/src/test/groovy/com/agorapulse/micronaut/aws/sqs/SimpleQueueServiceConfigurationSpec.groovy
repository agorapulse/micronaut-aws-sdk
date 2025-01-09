/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            context.getBean(SimpleQueueService)
            context.getBean(SimpleQueueService).defaultQueueName == 'DefaultQueue'
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue'))
            context.getBean(SimpleQueueService, Qualifiers.byName('samplequeue')).defaultQueueName == 'SampleQueue'
    }

}
