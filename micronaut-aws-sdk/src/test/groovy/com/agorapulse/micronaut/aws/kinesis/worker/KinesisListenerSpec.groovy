package com.agorapulse.micronaut.aws.kinesis.worker

import com.agorapulse.micronaut.aws.kinesis.*
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.amazonaws.services.dynamodbv2.model.TableDescription
import com.amazonaws.services.dynamodbv2.model.TableStatus
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.Record
import com.fasterxml.jackson.databind.ObjectMapper
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

import javax.inject.Singleton
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@Testcontainers
@RestoreSystemProperties
class KinesisListenerSpec extends Specification {

    public static final String TEST_STREAM = 'TestStream'
    public static final String APP_NAME = 'AppName'

    @Shared
    LocalStackContainer localstack = new LocalStackContainer('0.8.8')
        .withServices(LocalStackContainer.Service.KINESIS)
        .withEnv(DEBUG: '1')

    @AutoCleanup
    ApplicationContext context

    AmazonKinesis kinesis
    AmazonDynamoDB dynamo
    KinesisService kinesisService

    void setup() {
        // disable CBOR (not supported by Kinelite)
        System.setProperty('com.amazonaws.sdk.disableCbor', 'true')

        kinesis = AmazonKinesisClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        kinesisService = new DefaultKinesisService(this.kinesis, new KinesisConfiguration(stream: TEST_STREAM), new ObjectMapper())
        kinesisService.createStream()

        while (kinesisService.describeStream().streamDescription.streamStatus != 'ACTIVE') {
            Thread.sleep(100)
        }

        assert kinesisService.listShards().size() == 1

        dynamo = Mock(AmazonDynamoDB)
    }

    void 'kinesis listener is executed'() {
        given:
            Map<String, Map<String, AttributeValue>> leases = [:]
        when:
            context = ApplicationContext.build().properties(
                'aws.kinesis.application.name': APP_NAME,
                'aws.kinesis.client.stream': TEST_STREAM,
                'aws.kinesis.client.kinesisEndpoint': localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS).serviceEndpoint,
            ).build()
            context.registerSingleton(AmazonDynamoDB, dynamo)
            context.start()

            KinesisListenerTester tester = context.getBean(KinesisListenerTester)

            Disposable subscription = Flowable
                .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
                .takeWhile { tester.executions.size() < 5 }
                .subscribe {
                    try {
                        kinesisService.putEvent(new MyEvent(value: 'foo'))

                    } catch (Exception e) {
                        if (e.message.contains('Unable to execute HTTP request')) {
                            // already finished
                            return
                        }
                        throw e
                    }
                }

            600.times {
                if (tester.executions.size() < 5) {
                    Thread.sleep(100)
                }
            }

            subscription.dispose()
        then:
            tester.executions
            tester.executions.size() >= 5
            tester.executions.any { it?.startsWith('EXECUTED: listenStringRecord') }
            tester.executions.any { it?.startsWith('EXECUTED: listenString') }
            tester.executions.any { it?.startsWith('EXECUTED: listenRecord') }
            tester.executions.any { it?.startsWith('EXECUTED: listenObject') }
            tester.executions.any { it?.startsWith('EXECUTED: listenObjectRecord') }

            _ * dynamo.setRegion(_)
            _ * dynamo.describeTable(_) >> new DescribeTableResult().withTable(new TableDescription().withTableStatus(TableStatus.ACTIVE))

            _ * dynamo.scan(_) >> {
                new ScanResult(items: leases.values())
            }

            1 * dynamo.putItem(_ as PutItemRequest) >> { PutItemRequest request ->
                String leaseKey = request.item.leaseKey.s
                leases[leaseKey] = request.item
                return new PutItemResult()
            }

            _ * dynamo.updateItem(_ as UpdateItemRequest) >> { UpdateItemRequest request ->
                String leaseKey = request.key.leaseKey.s
                for (Map.Entry<String, AttributeValueUpdate> e : request.attributeUpdates.entrySet()) {
                    leases[leaseKey].put(e.key, e.value.value)
                }
                return new UpdateItemResult()
            }

            _ * dynamo.getItem(_ as GetItemRequest) >> { GetItemRequest request ->
                String leaseKey = request.key.leaseKey.s
                return new GetItemResult().withItem(leases[leaseKey])
            }

            0 * dynamo._
    }

}

class MyEvent extends AbstractEvent {
    String value
}

@Singleton
class KinesisListenerTester {

    List<String> executions = new CopyOnWriteArrayList()

    @KinesisListener
    void listenStringRecord(String string, Record record) {
        executions << "EXECUTED: listenStringRecord($string, $record)".toString()
    }

    @KinesisListener
    void listenString(String string) {
        executions << "EXECUTED: listenString($string)".toString()
    }

    @KinesisListener
    void listenRecord(Record record) {
        executions << "EXECUTED: listenRecord($record)\n".toString()
    }

    @KinesisListener
    void listenObject(MyEvent event) {
        executions << "EXECUTED: listenObject($event)".toString()
    }

    @KinesisListener
    void listenObjectRecord(MyEvent event, Record record) {
        executions << "EXECUTED: listenObjectRecord($event, $record)".toString()
    }

}
