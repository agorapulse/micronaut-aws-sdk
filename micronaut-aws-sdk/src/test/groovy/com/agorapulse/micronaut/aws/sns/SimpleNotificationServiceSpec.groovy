package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@Testcontainers
class SimpleNotificationServiceSpec extends Specification {


    @Shared
    LocalStackContainer localstack = new LocalStackContainer().withServices(LocalStackContainer.Service.SNS)

    AmazonSNS sns
    SimpleNotificationService service

    void setup() {
        sns = AmazonSNSClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.SNS))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()
        service = new DefaultSimpleNotificationService(sns, new SimpleNotificationServiceConfiguration(), new ObjectMapper())
    }

    void 'create topic'() {
        when:
            String topicArn = service.createTopic('TOPIC')
        then:
            topicArn
    }

    void 'publish message'() {
        when:
            String messageId = service.publishTopic('TOPIC', 'SUBJECT', 'MESSAGE')
        then:
            messageId
    }


    void 'delete topic'() {
        when:
            service.deleteTopic(sns.listTopics().topics.first().topicArn)
        then:
            noExceptionThrown()
    }
}
