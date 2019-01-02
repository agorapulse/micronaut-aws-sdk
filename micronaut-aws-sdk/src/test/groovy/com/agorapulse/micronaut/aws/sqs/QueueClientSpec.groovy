package com.agorapulse.micronaut.aws.sqs

import com.agorapulse.micronaut.aws.Pogo
import com.agorapulse.micronaut.aws.sqs.annotation.Queue
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests or queue client.
 */
class QueueClientSpec extends Specification {

    private static final String DEFAULT_QUEUE_NAME = 'DefaultQueue'
    private static final String MESSAGE = 'MESSAGE'
    private static final Pogo POGO = new Pogo(foo: 'bar')
    private static final String GROUP = 'SomeGroup'
    private static final int DELAY = 10
    private static final String ID = '12345'

    SimpleQueueService defaultService = Mock(SimpleQueueService) {
        getDefaultQueueName() >> DEFAULT_QUEUE_NAME
    }

    SimpleQueueService testService = Mock(SimpleQueueService) {
        getDefaultQueueName() >> DEFAULT_QUEUE_NAME
    }

    @AutoCleanup ApplicationContext context

    String marshalledPogo

    void setup() {
        context = ApplicationContext.build().build()

        context.registerSingleton(SimpleQueueService, defaultService, Qualifiers.byName('default'))
        context.registerSingleton(SimpleQueueService, testService, Qualifiers.byName('test'))

        context.start()

        marshalledPogo = context.getBean(ObjectMapper).writeValueAsString(POGO)
    }

    void 'can send message to other than default queue potentionally altering the group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessageToQueue(MESSAGE)
        then:
            id == ID

            1 * defaultService.sendMessage(DefaultClient.OTHER_QUEUE, MESSAGE, 0, GROUP) >> ID
    }

    void 'can send a single message and return id'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, marshalledPogo, 0, null) >> ID
    }

    void 'can send a single message and return id with specified configuration name'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * testService.sendMessage(DEFAULT_QUEUE_NAME, marshalledPogo, 0, null) >> ID
    }

    void 'can send a single byte array message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE.bytes)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, null) >> ID
    }

    void 'can send a single string message'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, null) >> ID
    }

    void 'can send a single string message with delay'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, DELAY)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, DELAY, null) >> ID
    }

    void 'can send a single string message with group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, GROUP)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, 0, GROUP) >> ID
    }

    void 'can send a single string message with delay and group'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            String id = client.sendMessage(MESSAGE, DELAY, GROUP)
        then:
            id == ID

            1 * defaultService.sendMessage(DEFAULT_QUEUE_NAME, MESSAGE, DELAY, GROUP) >> ID
    }

    void 'needs to follow the method convention rules'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.doWhatever(POGO, POGO, POGO, POGO)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'can delete message by id'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.deleteMessage(ID)
        then:
            1 * defaultService.deleteMessage(DEFAULT_QUEUE_NAME, ID)
    }

    void 'can send message with specified queue name'() {
        given:
            QueueClient client = context.getBean(QueueClient)
        when:
            String id = client.sendMessage(POGO)
        then:
            id == ID

            1 * defaultService.sendMessage(QueueClient.SOME_QUEUE, marshalledPogo, DELAY, null) >> ID
    }
}

@com.agorapulse.micronaut.aws.sqs.annotation.QueueClient interface DefaultClient {

    public String OTHER_QUEUE = 'OtherQueue'

    @Queue(value = 'OtherQueue', group = 'SomeGroup') String sendMessageToQueue(String message)

    String sendMessage(Pogo message)

    String sendMessage(byte[] record)
    String sendMessage(String record)
    String sendMessage(String record, int delay)
    String sendMessage(String record, String group)
    String sendMessage(String record, int delay, String group)

    // fails
    void doWhatever(Object one, Object two, Object three, Object four)

    // delete
    void deleteMessage(String messageId)
}

@com.agorapulse.micronaut.aws.sqs.annotation.QueueClient('test') interface TestClient {
    String sendMessage(Pogo event)
}

@com.agorapulse.micronaut.aws.sqs.annotation.QueueClient(queue = 'SomeQueue', delay = 10) interface QueueClient {

    public String SOME_QUEUE = 'SomeQueue'

    String sendMessage(Pogo event)
}

