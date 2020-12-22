/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import spock.lang.AutoCleanup
import spock.lang.Retry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.environment.RestoreSystemProperties

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS

/**
 * Tests for simple queue service.
 */

@Retry(count =  5)
@Stepwise

// tag::testcontainers-header[]
@Testcontainers                                                                         // <1>
@RestoreSystemProperties                                                                // <2>
class SimpleQueueServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final String TEST_QUEUE = 'TestQueue'
    private static final String DATA = 'Hello World'

    // tag::testcontainers-fields[]
    @Shared LocalStackContainer localstack = new LocalStackContainer()                  // <3>
        .withServices(SQS)

    @AutoCleanup ApplicationContext context                                             // <4>

    SimpleQueueService service
    // end::testcontainers-fields[]

    // tag::testcontainers-setup[]
    void setup() {
        System.setProperty('com.amazonaws.sdk.disableCbor', 'true')                     // <5>
        SqsClient sqs = SqsClient                                                       // <6>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(SQS))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getAccessKey(), localstack.getSecretKey()
            )))
            .region(Region.of(localstack.region))
            .build()

        context = ApplicationContext.build('aws.sqs.queue': TEST_QUEUE).build()         // <7>
        context.registerSingleton(SqsClient, sqs)
        context.start()

        service = context.getBean(SimpleQueueService)                                   // <8>
    }
    // end::testcontainers-setup[]

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
            service.deleteMessage(msgId)
        then:
            !service.receiveMessages()

        when:
            service.deleteQueue(TEST_QUEUE)
        then:
            !service.listQueueUrls().contains(queueUrl)
    }

}
