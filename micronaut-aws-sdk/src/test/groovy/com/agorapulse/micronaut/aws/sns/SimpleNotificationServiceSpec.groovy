package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS

@Stepwise
@Testcontainers
class SimpleNotificationServiceSpec extends Specification {

    @Shared LocalStackContainer localstack = new LocalStackContainer('0.8.8').withServices(SNS).withEnv('DEBUG', '1')
    @Shared SimpleNotificationServiceConfiguration configuration = new SimpleNotificationServiceConfiguration()
    @Shared String endpointArn
    @Shared String platformApplicationArn
    @Shared String subscriptionArn
    @Shared String topicArn

    AmazonSNS sns
    SimpleNotificationService service

    void setup() {
        sns = AmazonSNSClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(SNS))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()
        service = new DefaultSimpleNotificationService(sns, configuration, new ObjectMapper())
    }

    void 'create topic'() {
        when:
            topicArn = service.createTopic('TOPIC')
        then:
            topicArn
    }

    void 'subscribe to the topic'() {
        when:
            subscriptionArn = service.subscribeTopicWithEmail(topicArn, 'vlad@agorapulse.com')
        then:
            subscriptionArn
    }

    void 'create platform application'() {
        when:
            platformApplicationArn = service.createAndroidApplication('ANDROID-APP', 'API-KEY')
        then:
            platformApplicationArn

        when:
            configuration.android.arn = platformApplicationArn
        then:
            noExceptionThrown()
    }

    void 'register device'() {
        when:
            endpointArn = service.registerAndroidDevice('TOKEN', 'CUSTOMER-DATA')
        then:
            endpointArn
    }

    void 'publish message'() {
        when:
            String messageId = service.publishMessageToTopic(topicArn, 'SUBJECT', 'MESSAGE')
        then:
            messageId
    }

    void 'validate device'() {
        when:
            String newEndpointArn = service.validateAndroidDevice(endpointArn, 'OTHER-TOKEN', 'OTHER-CUSTOMER-DATA')
        then:
            endpointArn == newEndpointArn
    }

    void 'publish direct'() {
        when:
            String messageId = service.sendAndroidAppNotification(endpointArn, [message: 'Hello'], 'key')
        then:
            messageId
    }

    void 'unregister device'() {
        when:
            service.unregisterDevice(endpointArn)
        then:
            noExceptionThrown()
    }

    void 'unsubscribe from the topic'() {
        when:
            service.unsubscribeTopic(subscriptionArn)
        then:
            noExceptionThrown()
    }

    void 'delete topic'() {
        when:
            service.deleteTopic(topicArn)
        then:
            noExceptionThrown()
    }
}
