/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker

import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisService
import io.micronaut.context.ApplicationContext
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.http.Protocol
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.utils.AttributeMap
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.util.concurrent.TimeUnit

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS

/**
 * Tests for Kinesis related annotations - client and listener.
 */

@SuppressWarnings('ClassStartsWithBlankLine')

// tag::testcontainers-header[]
@Testcontainers                                                                         // <1>
@RestoreSystemProperties                                                                // <2>
class KinesisAnnotationsSpec extends Specification {
// end::testcontainers-header[]

    // tag::testcontainers-setup[]
    private static final String TEST_STREAM = 'TestStream'
    private static final String APP_NAME = 'AppName'

    @Shared LocalStackContainer localstack = new LocalStackContainer()                  // <3>
        .withServices(KINESIS, DYNAMODB, CLOUDWATCH)

    @AutoCleanup ApplicationContext context                                             // <4>

    @SuppressWarnings(['AbcMetric', 'UnnecessaryObjectReferences'])
    void setup() {
        // disable CBOR (not supported by Kinelite)
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), 'false')           // <5>
        System.setProperty('aws.region', 'eu-west-1')

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            localstack.accessKey, localstack.secretKey
        ))

        SdkAsyncHttpClient asyncClient = NettyNioAsyncHttpClient
            .builder()
            .protocol(Protocol.HTTP1_1)
            .buildWithDefaults(
                AttributeMap
                    .builder()
                    .put(
                        SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
                        Boolean.TRUE
                    )
                    .build()
            )

        DynamoDbAsyncClient dynamoAsync = DynamoDbAsyncClient                            // <6>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
            .credentialsProvider(credentialsProvider)
            .region(Region.EU_WEST_1)
            .httpClient(asyncClient)
            .build()

        DynamoDbClient dynamo = DynamoDbClient
            .builder()
            .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
            .credentialsProvider(credentialsProvider)
            .region(Region.EU_WEST_1)
            .build()

        KinesisAsyncClient kinesisAsync = KinesisAsyncClient                             // <7>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(credentialsProvider)
            .region(Region.EU_WEST_1)
            .httpClient(asyncClient)
            .build()

        KinesisClient kinesis = KinesisClient
            .builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(credentialsProvider)
            .region(Region.EU_WEST_1)
            .build()

        CloudWatchAsyncClient amazonCloudWatch = CloudWatchAsyncClient
            .builder()
            .endpointOverride(localstack.getEndpointOverride(CLOUDWATCH))
            .credentialsProvider(credentialsProvider)
            .region(Region.EU_WEST_1)
            .httpClient(asyncClient)
            .build()

        context = ApplicationContext.builder().properties(                              // <8>
            'aws.kinesis.application.name': APP_NAME,
            'aws.kinesis.stream': TEST_STREAM,
            'aws.kinesis.listener.stream': TEST_STREAM,
            'aws.kinesis.listener.failoverTimeMillis': '1000',
            'aws.kinesis.listener.shardSyncIntervalMillis': '1000',
            'aws.kinesis.listener.idleTimeBetweenReadsInMillis': '1000',
            'aws.kinesis.listener.parentShardPollIntervalMillis': '1000',
            'aws.kinesis.listener.timeoutInSeconds': '1000',
            'aws.kinesis.listener.retryGetRecordsInSeconds': '1000'
        ).build()
        context.registerSingleton(KinesisClient, kinesis)
        context.registerSingleton(KinesisAsyncClient, kinesisAsync)
        context.registerSingleton(DynamoDbClient, dynamo)
        context.registerSingleton(DynamoDbAsyncClient, dynamoAsync)
        context.registerSingleton(CloudWatchAsyncClient, amazonCloudWatch)
        context.registerSingleton(AwsCredentialsProvider, credentialsProvider)
        context.start()
    }
    // end::testcontainers-setup[]

    void cleanup() {
        System.clearProperty(SdkSystemSetting.CBOR_ENABLED.property())
        System.clearProperty('aws.region')
    }

    // tag::testcontainers-test[]
    void 'kinesis listener is executed'() {
        when:
            KinesisService service = context.getBean(KinesisService)                    // <9>
            KinesisListenerTester tester = context.getBean(KinesisListenerTester)       // <10>
            DefaultClient client = context.getBean(DefaultClient)                       // <11>

            service.createStream()
            service.waitForActive()

            waitForWorkerReady(1200, 100)

            Disposable subscription = publishEventAsync(tester, client)

            waitForReceivedMessages(tester, 300, 100)

            subscription.dispose()
        then:
            allTestEventsReceived(tester)
        cleanup:
            context.getBean(KinesisListenerMethodProcessor).close()
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
        return Flowable
            .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .takeWhile {
                !allTestEventsReceived(tester)
            }.subscribe {
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

    @SuppressWarnings(['SystemErrPrint', 'VariableName'])
    private static boolean allTestEventsReceived(KinesisListenerTester tester) {
        final int EXPECTED = 6
        int passedEvents = 0
        if (tester.executions.any { it?.startsWith('EXECUTED: listenStringRecord') }) {
            System.err.println('Verfifed listenStringRecord OK')
            passedEvents++
        }

        if (tester.executions.any { it?.startsWith('EXECUTED: listenString') }) {
            System.err.println('Verfifed listenString OK')
            passedEvents++
        }

        if (tester.executions.any { it?.startsWith('EXECUTED: listenRecord') }) {
            System.err.println('Verfifed listenRecord OK')
            passedEvents++
        }

        if (tester.executions.any { it?.startsWith('EXECUTED: listenObject') }) {
            System.err.println('Verfifed listenObject OK')
            passedEvents++
        }

        if (tester.executions.any { it?.startsWith('EXECUTED: listenObjectRecord') }) {
            System.err.println('Verfifed listenObjectRecord OK')
            passedEvents++
        }

        if (tester.executions.any { it == 'EXECUTED: listenPogoRecord(com.agorapulse.micronaut.amazon.awssdk.kinesis.worker.Pogo(bar))' }) {
            System.err.println('Verfifed listenPogoRecord OK')
            passedEvents++
        }

        System.err.println("Passed $passedEvents event checks of $EXPECTED")

        return passedEvents == EXPECTED
    }

    @SuppressWarnings('SystemErrPrint')
    private void waitForWorkerReady(int retries, int waitMillis) throws InterruptedException {
        WorkerStateListener listener = context.getBean(WorkerStateListener)
        for (int i = 0; i < retries; i++) {
            if (!listener.isReady(TEST_STREAM)) {
                Thread.sleep(waitMillis)
            }
        }
        if (!listener.isReady(TEST_STREAM)) {
            throw new IllegalStateException("Worker not ready yet after ${retries * waitMillis} milliseconds")
        }
        System.err.println('Worker is ready')
        Thread.sleep(waitMillis)
    }

}
