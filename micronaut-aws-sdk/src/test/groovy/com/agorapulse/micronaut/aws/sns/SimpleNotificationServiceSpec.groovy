package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS

/**
 * Tests for simple notification service.
 */
@Stepwise
// tag::testcontainers-header[]
@Testcontainers                                                                         // <1>
class SimpleNotificationServiceSpec extends Specification {
// end::testcontainers-header[]

    private static final String TEST_TOPIC = 'TestTopic'

    // tag::testcontainers-fields[]
    @Shared LocalStackContainer localstack = new LocalStackContainer('0.8.10')          // <2>
        .withServices(SNS)

    @AutoCleanup ApplicationContext context                                             // <3>

    SimpleNotificationService service
    // end::testcontainers-fields[]

    @Shared String endpointArn
    @Shared String platformApplicationArn
    @Shared String subscriptionArn
    @Shared String topicArn

    // tag::testcontainers-setup[]
    void setup() {
        AmazonSNS sns = AmazonSNSClient                                                 // <4>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(SNS))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        context = ApplicationContext.build('aws.sns.topic', TEST_TOPIC).build()         // <5>
        context.registerSingleton(AmazonSNS, sns)
        context.start()

        service = context.getBean(SimpleNotificationService)                            // <6>
    }
    // end::testcontainers-setup[]

    void 'new topic'() {
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

    void 'new platform application'() {
        when:
            platformApplicationArn = service.createAndroidApplication('ANDROID-APP', 'API-KEY')
        then:
            platformApplicationArn
    }

    void 'register device'() {
        when:
            endpointArn = service.registerAndroidDevice(platformApplicationArn, 'TOKEN', 'CUSTOMER-DATA')
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
            String newEndpointArn = service.validateAndroidDevice(platformApplicationArn, endpointArn, 'OTHER-TOKEN', 'OTHER-CUSTOMER-DATA')
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
