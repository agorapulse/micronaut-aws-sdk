package com.agorapulse.micronaut.aws.kinesis

import com.agorapulse.micronaut.aws.Pogo
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClient
import io.micronaut.context.ApplicationContext
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS

/**
 * Tests for Kinesis related annotations - client and listener.
 */
// tag::testcontainers-header[]
@Testcontainers                                                                         // <1>
@RestoreSystemProperties                                                                // <2>
class KinesisAnnotationsSpec extends Specification {
// end::testcontainers-header[]

    // tag::testcontainers-setup[]
    private static final String TEST_STREAM = 'TestStream'
    private static final String APP_NAME = 'AppName'

    @Shared LocalStackContainer localstack = new LocalStackContainer('0.8.8')   // <3>
        .withServices(KINESIS, DYNAMODB)

    @AutoCleanup ApplicationContext context                                             // <4>

    void setup() {
        System.setProperty('com.amazonaws.sdk.disableCbor', 'true')                     // <5>

        AmazonDynamoDB dynamo = AmazonDynamoDBClient                                    // <6>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        AmazonKinesis kinesis = AmazonKinesisClient                                     // <7>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(KINESIS))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        context = ApplicationContext.build().properties(                                // <8>
            'aws.kinesis.application.name': APP_NAME,
            'aws.kinesis.stream': TEST_STREAM,
            'aws.kinesis.listener.stream': TEST_STREAM
        ).build()
        context.registerSingleton(AmazonKinesis, kinesis)
        context.registerSingleton(AmazonDynamoDB, dynamo)
        context.start()
    }
    // end::testcontainers-setup[]

    // tag::testcontainers-test[]
    void 'kinesis listener is executed'() {
        when:
            KinesisService service = context.getBean(KinesisService)                    // <9>
            KinesisListenerTester tester = context.getBean(KinesisListenerTester)       // <10>
            DefaultClient client = context.getBean(DefaultClient)                       // <11>

            service.createStream()
            service.waitForActive()

            Disposable subscription = publishEventAsync(tester, client)

            waitForReceivedMessages(tester, 120, 1000)

            subscription.dispose()
        then:
            allTestEventsReceived(tester)
    }
    // end::testcontainers-test[]

    private static void waitForReceivedMessages(KinesisListenerTester tester, int retries, int waitMillis) {
        for (int i = 0; i < retries; i++) {
            if (!allTestEventsReceived(tester)) {
                Thread.sleep(waitMillis)
            }
        }
    }

    @SuppressWarnings('CatchException')
    private static Disposable publishEventAsync(KinesisListenerTester tester, DefaultClient client) {
        Flowable
            .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .takeWhile {
                !allTestEventsReceived(tester)
            } subscribe {
            try {
                client.putEvent(new MyEvent(value: 'foo'))
                client.putRecordDataObject('1234567890', new Pogo(foo: 'bar'))
            } catch (Exception e) {
                if (e.message.contains('Unable to execute HTTP request')) {
                    // already finished
                    return
                }
                throw e
            }
        }
    }

    private static boolean allTestEventsReceived(KinesisListenerTester tester) {
        return tester.executions.any { it?.startsWith('EXECUTED: listenStringRecord') } &&
            tester.executions.any { it?.startsWith('EXECUTED: listenString') } &&
            tester.executions.any { it?.startsWith('EXECUTED: listenRecord') } &&
            tester.executions.any { it?.startsWith('EXECUTED: listenObject') } &&
            tester.executions.any { it?.startsWith('EXECUTED: listenObjectRecord') } &&
            tester.executions.any { it == 'EXECUTED: listenPogoRecord(com.agorapulse.micronaut.aws.Pogo(bar))' }
    }

}
