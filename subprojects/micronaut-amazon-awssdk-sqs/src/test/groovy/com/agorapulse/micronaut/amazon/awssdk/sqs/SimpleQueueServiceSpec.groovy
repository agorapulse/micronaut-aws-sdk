/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.sqs

import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import spock.lang.Retry
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject

/**
 * Tests for simple queue service.
 */

@Retry(count =  5)
@Stepwise

// tag::header[]
@MicronautTest                                                                          // <1>
@Property(name = 'aws.sqs.queue', value = TEST_QUEUE)                                   // <2>
class SimpleQueueServiceSpec extends Specification {

// end::header[]

    private static final String TEST_QUEUE = 'TestQueueGroovy'
    private static final String DATA = 'Hello World'

    // tag::setup[]
    @Inject SimpleQueueService service                                                  // <3>
    // end::setup[]

    void 'working with queues'() {
        when:
            String queueUrl = service.createQueue(TEST_QUEUE)
        then:
            queueUrl
            service.listQueueUrls().contains(queueUrl)

        when:
            Map<QueueAttributeName, String> queueAttributes = service.getQueueAttributes(TEST_QUEUE)
        then:
            queueAttributes[QueueAttributeName.DELAY_SECONDS] == '0'

        when:
            String msgId = service.sendMessage(DATA)
            List<Message> messages = service.receiveMessages()
        then:
            msgId
            messages
            messages.first().body() == DATA
            messages.first().messageId() == msgId

        when:
            service.deleteMessage(messages.first().receiptHandle())
        then:
            !service.receiveMessages()

        when:
            service.deleteQueue(TEST_QUEUE)
        then:
            !service.listQueueUrls().contains(queueUrl)
    }

}
