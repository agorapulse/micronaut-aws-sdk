/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.CreateQueueResult
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.QueueDoesNotExistException
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

/**
 * Tests for simple queue service.
 */
class SimpleQueueServiceWithMockSpec extends Specification {

    SimpleQueueServiceConfiguration configuration = new DefaultSimpleQueueServiceConfiguration(cache: true)
    AmazonSQS amazonSQS = Mock(AmazonSQS)
    SimpleQueueService service = new DefaultSimpleQueueService(amazonSQS, configuration)

    /*
     * Tests for createQueue(String queueName)
     */

    void 'Create queue'() {
        when:
            configuration.delaySeconds = 100
            String queueUrl = service.createQueue('someQueue')

        then:
            1 * amazonSQS.createQueue(_) >> ['queueUrl': 'somepath/queueName']
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
            1 * amazonSQS.deleteQueue('somepath/queueName')
    }

    /*
     * Tests for listQueueNames()
     */

    void 'List queue names'() {
        when:
            List queueNames = service.listQueueNames()

        then:
            1 * amazonSQS.listQueues(_) >> ['queueUrls': ['somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3']]
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
            1 * amazonSQS.listQueues(_) >> ['queueUrls': ['somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3']]

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
                GetQueueAttributesResult attrResult = new GetQueueAttributesResult()
                attrResult.attributes = ['attribute1': 'value1', 'attribute2': 'value2']
                attrResult
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
            1 * amazonSQS.getQueueAttributes(_, ['QueueArn']) >> {
                GetQueueAttributesResult attrResult = new GetQueueAttributesResult()
                attrResult.attributes = ['QueueArn': 'arn:sqs:queueName']
                attrResult
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
                AmazonServiceException exception = new AmazonServiceException('Error')
                exception.errorCode = 'AWS.SimpleQueueService.NonExistentQueue'
                throw exception
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
            1 * amazonSQS.getQueueAttributes(_) >> { throw new AmazonClientException('Error') }

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
            amazonSQS.listQueues(_) >> ['queueUrls': ['somepath/queueName1', 'somepath/queueName2', 'somepath/queueName3']]
            amazonSQS.createQueue(_) >> ['queueUrl': 'somepath/queueName']
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
            thrown(AmazonSQSException)

            1 * amazonSQS.getQueueUrl('vlad_queueName') >> { throw new QueueDoesNotExistException('Queue does not exist') }
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
            1 * amazonSQS.receiveMessage(_) >> ['messages': ['message1', 'message2']]
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
            1 * amazonSQS.sendMessage(_) >> ['messageId': 'msg_id']

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
            context.registerSingleton(AmazonSQS, amazonSQS)
            context.start()

            SimpleQueueService service = context.getBean(SimpleQueueService)
            SimpleQueueServiceConfiguration configuration = context.getBean(SimpleQueueServiceConfiguration)
            service.createQueue('queueName')
        then:
            noExceptionThrown()

            configuration
            service

            amazonSQS.createQueue(_) >> new CreateQueueResult().withQueueUrl('http://test.example.com/my-queue')

        cleanup:
            context.stop()
    }

}
