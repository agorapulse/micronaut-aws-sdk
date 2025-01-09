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
package com.agorapulse.micronaut.amazon.awssdk.sqs

import io.micronaut.context.ApplicationContext
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import spock.lang.Specification

/**
 * Tests for simple queue service.
 */
class SimpleQueueServiceWithMockSpec extends Specification {

    SimpleQueueServiceConfiguration configuration = new DefaultSimpleQueueServiceConfiguration(cache: true)
    SqsClient amazonSQS = Mock()
    SimpleQueueService service = new DefaultSimpleQueueService(amazonSQS, configuration)

    /*
     * Tests for createQueue(String queueName)
     */

    void 'Create queue'() {
        when:
            configuration.delaySeconds = 100
            String queueUrl = service.createQueue('someQueue')

        then:
            1 * amazonSQS.createQueue(_) >> CreateQueueResponse.builder().queueUrl('somepath/queueName').build()
            queueUrl
            queueUrl == 'somepath/queueName'
    }

    /*
     * Tests for deleteMessage(String queueUrl, String receiptHandle)
     */

    void 'Delete message'() {
        given:
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            service.deleteMessage('queueName', 'receiptHandle')

        then:
            1 * amazonSQS.deleteMessage(_)
    }

    void 'Delete message in default queue'() {
        given:
            configuration.queue = 'queueName'
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            service.deleteMessage('receiptHandle')

        then:
            1 * amazonSQS.deleteMessage(_)
    }

    void 'Delete queue'() {
        given:
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            service.deleteQueue('queueName')

        then:
            1 * amazonSQS.deleteQueue(_)
    }

    /*
     * Tests for listQueueNames()
     */

    void 'List queue names'() {
        when:
            List queueNames = service.listQueueNames()

        then:
            1 * amazonSQS.listQueues(_) >> ListQueuesResponse.builder().queueUrls('somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3').build()
            queueNames
            queueNames.size() == 3
            queueNames == ['queueName1', 'queueName2', 'queueName3']
    }

    /*
     * Tests for listQueueUrls()
     */

    void 'List queue urls'() {
        when:
            List queueUrls = service.listQueueUrls()

        then:
            1 * amazonSQS.listQueues(_) >> ListQueuesResponse.builder().queueUrls('somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3').build()

            queueUrls
            queueUrls.size() == 3
            queueUrls == ['somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3']
    }

    /*
     * Tests for getQueueAttributes(String queueUrl)
     */

    void 'Get queue attributes'() {
        given:
            configuration.queue = 'queueName'
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            Map attributes = service.queueAttributes

        then:
            1 * amazonSQS.getQueueAttributes(_) >> {
                GetQueueAttributesResponse
                    .builder()
                    .attributes((QueueAttributeName.DELAY_SECONDS): '10', (QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES): '50')
                    .build()
            }

            attributes
            attributes.size() == 2
    }

    /*
    * Tests for getQueueAttributes(String queueUrl)
    */

    void 'Get queue arn'() {
        given:
            configuration.queue = 'queueName'
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            String arn = service.getQueueArn('queueName')

        then:
            1 * amazonSQS.getQueueAttributes(_) >> {
                GetQueueAttributesResponse
                    .builder()
                    .attributes((QueueAttributeName.QUEUE_ARN): 'arn:sqs:queueName')
                    .build()
            }

            arn == 'arn:sqs:queueName'
    }

    void 'Get queue attributes service exception'() {
        given:
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            Map attributes = service.getQueueAttributes('queueName')

        then:
            1 * amazonSQS.getQueueAttributes(_) >> {
                throw AwsServiceException
                    .builder()
                    .message('Error')
                    .awsErrorDetails(AwsErrorDetails.builder().errorCode('AWS.SimpleQueueService.NonExistentQueue').build())
                    .build()
            }

            !attributes
            old(service.queueUrlByNames.size() == 1)
            service.queueUrlByNames.size() == 0
    }

    void 'Get queue attributes client exception'() {
        given:
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            Map attributes = service.getQueueAttributes('queueName')

        then:
            1 * amazonSQS.getQueueAttributes(_) >> { throw SdkClientException.builder().message('Error').build() }

            !attributes
    }

    /*
     * Tests for getQueueUrl()
     */

    void 'Get queue url with autocreate'() {
        given:
            configuration.autoCreateQueue = true
            configuration.queue = 'queueName'

        when:
            String queueUrl = service.queueUrl

        then:
            amazonSQS.listQueues(_) >> ListQueuesResponse.builder().queueUrls('somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3').build()
            amazonSQS.createQueue(_) >> CreateQueueResponse.builder().queueUrl('somepath/queueName').build()
            amazonSQS
            queueUrl == 'somepath/queueName'
            old(service.queueUrlByNames.size() == 0)
            service.queueUrlByNames.size() == 4
            service.queueUrlByNames['queueName'] == 'somepath/queueName'
    }

    void 'Get queue url without autocreate'() {
        given:
            configuration.queueNamePrefix = 'vlad_'
            configuration.cache = false

        when:
            service.getQueueUrl('queueName')

        then:
            thrown(AwsServiceException)

            1 * amazonSQS.getQueueUrl(_) >> { throw  QueueDoesNotExistException.builder().message('Queue does not exist').build() }
    }

    /*
     * Tests for receiveMessages(String queueUrl, int maxNumberOfMessages = 0, int visibilityTimeout = 0, int waitTimeSeconds = 0)
     */

    void 'Receive messages'() {
        given:
            configuration.queue = 'queueName'
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            List messages = service.receiveMessages(1, 1, 1)

        then:
            1 * amazonSQS.receiveMessage(_) >> ReceiveMessageResponse.builder().messages(
                Message.builder().body('message1').build(),
                Message.builder().body('message2').build()
            ).build()
            messages
            messages.size() == 2
    }

    /*
     * Tests for sendMessage(String queueUrl, String messageBody)
     */

    void 'Send messages'() {
        given:
            service.queueUrlByNames['queueName'] = 'somepath/queueName'

        when:
            String messageId = service.sendMessage('queueName', 'messageBody')

        then:
            1 * amazonSQS.sendMessage(_) >> SendMessageResponse.builder().messageId('msg_id').build()

            messageId == 'msg_id'
    }

    void 'queue name can be url'() {
        given:
            String myQueueUrl = 'http://test.example.com/my-queue'
        when:
            service.queueUrlByNames['my-queue'] = myQueueUrl
            String url = service.getQueueUrl('my-queue')
        then:
            url == myQueueUrl
        and:
            service.getQueueUrl(myQueueUrl) == myQueueUrl
    }

    void 'integration spec'() {
        when:
            ApplicationContext context = ApplicationContext.builder().build()
            context.registerSingleton(SqsClient, amazonSQS)
            context.start()

            SimpleQueueService service = context.getBean(SimpleQueueService)
            SimpleQueueServiceConfiguration configuration = context.getBean(SimpleQueueServiceConfiguration)
            service.createQueue('queueName')
        then:
            noExceptionThrown()

            configuration
            service

            amazonSQS.createQueue(_) >> CreateQueueResponse.builder().queueUrl('http://test.example.com/my-queue').build()

        cleanup:
            context.stop()
    }

}
