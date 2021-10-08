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
package com.agorapulse.micronaut.aws.kinesis.worker;

import com.agorapulse.micronaut.aws.kinesis.KinesisService;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.cloudwatch.waiters.AmazonCloudWatchWaiters;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import io.micronaut.context.ApplicationContext;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.junit.*;
import org.mockito.Mockito;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

// tag::testcontainers-header[]
public class KinesisTest {
// end::testcontainers-header[]

    private static final String TEST_STREAM = "MyStream";

    @Rule
    public Retry retry = new Retry(10);

    // tag::testcontainers-setup[]
    public ApplicationContext context;                                                  // <1>

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()                   // <2>
        .withServices(DYNAMODB, KINESIS);

    @Before
    public void setup() {
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");                    // <3>
        System.setProperty("aws.region", "eu-west-1");

        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClient                            // <4>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        AmazonKinesis amazonKinesis = AmazonKinesisClient                               // <5>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(KINESIS))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        AmazonCloudWatch cloudWatch = Mockito.mock(AmazonCloudWatch.class);

        Map<String, Object> properties = new HashMap<>();                               // <6>
        properties.put("aws.kinesis.application.name", "TestApp");
        properties.put("aws.kinesis.stream", TEST_STREAM);
        properties.put("aws.kinesis.listener.stream", TEST_STREAM);

        // you can set other custom client configuration properties
        properties.put("aws.kinesis.listener.failoverTimeMillis", "1000");
        properties.put("aws.kinesis.listener.shardSyncIntervalMillis", "1000");
        properties.put("aws.kinesis.listener.idleTimeBetweenReadsInMillis", "1000");
        properties.put("aws.kinesis.listener.parentShardPollIntervalMillis", "1000");
        properties.put("aws.kinesis.listener.timeoutInSeconds", "1000");
        properties.put("aws.kinesis.listener.retryGetRecordsInSeconds", "1000");
        properties.put("aws.kinesis.listener.metricsLevel", "NONE");


        context = ApplicationContext.build(properties).build();                         // <7>
        context.registerSingleton(AmazonKinesis.class, amazonKinesis);
        context.registerSingleton(AmazonDynamoDB.class, amazonDynamoDB);
        context.registerSingleton(AmazonCloudWatch.class, cloudWatch);
        context.registerSingleton(AWSCredentialsProvider.class, localstack.getDefaultCredentialsProvider());
        context.start();
    }

    @After
    public void cleanup() {
        System.clearProperty("com.amazonaws.sdk.disableCbor");                          // <8>
        System.clearProperty("aws.region");
        if (context != null) {
            context.close();                                                            // <9>
        }
    }
    // end::testcontainers-setup[]

    // tag::testcontainers-test[]
    @Test
    public void testJavaService() throws InterruptedException {
        KinesisService service = context.getBean(KinesisService.class);                 // <10>
        KinesisListenerTester tester = context.getBean(KinesisListenerTester.class);    // <11>
        DefaultClient client = context.getBean(DefaultClient.class);                    // <12>

        service.createStream();
        service.waitForActive();

        waitForWorkerReady(300, 100);
        Disposable subscription = publishEventsAsync(tester, client);
        waitForRecievedMessages(tester, 300, 100);

        subscription.dispose();

        Assert.assertTrue(allTestEventsReceived(tester));
    }
    // end::testcontainers-test[]

    private void waitForRecievedMessages(KinesisListenerTester tester, int retries, int waitMillis) throws InterruptedException {
        for (int i = 0; i < retries; i++) {
            if (!allTestEventsReceived(tester)) {
                Thread.sleep(waitMillis);
            }
        }
    }

    private void waitForWorkerReady(int retries, int waitMillis) throws InterruptedException {
        WorkerStateListener listener = context.getBean(WorkerStateListener.class);
        for (int i = 0; i < retries; i++) {
            if (!listener.isReady(TEST_STREAM)) {
                Thread.sleep(waitMillis);
            }
        }
        if (!listener.isReady(TEST_STREAM)) {
            throw new IllegalStateException("Worker not ready yet after " + retries * waitMillis + " milliseconds");
        }
        System.err.println("Worker is ready");
        Thread.sleep(waitMillis);
    }

    private Disposable publishEventsAsync(KinesisListenerTester tester, DefaultClient client) {
        return Flowable
            .interval(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .takeWhile(t ->
                !allTestEventsReceived(tester)
            )
            .subscribe(t -> {
                try {
                    System.err.println("Publishing events");
                    client.putEvent(new MyEvent("foo"));
                    client.putRecordDataObject("1234567890", new Pogo("bar"));
                } catch (Exception e) {
                    if (e.getMessage().contains("Unable to execute HTTP request")) {
                        // already finished
                        return;
                    }
                    throw e;
                }
            });
    }

    private static boolean allTestEventsReceived(KinesisListenerTester tester) {
        final int expected = 6;
        int passedEvents = 0;
        if (tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenStringRecord"))) {
            System.err.println("Verfifed listenStringRecord OK");
            passedEvents++;
        }

        if (tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenString"))) {
            System.err.println("Verfifed listenString OK");
            passedEvents++;
        }

        if (tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenRecord"))) {
            System.err.println("Verfifed listenRecord OK");
            passedEvents++;
        }

        if (tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObject"))) {
            System.err.println("Verfifed listenObject OK");
            passedEvents++;
        }

        if (tester.getExecutions().stream().anyMatch(log -> log.startsWith("EXECUTED: listenObjectRecord"))) {
            System.err.println("Verfifed listenObjectRecord OK");
            passedEvents++;
        }

        if (tester.getExecutions().stream().anyMatch("EXECUTED: listenPogoRecord(com.agorapulse.micronaut.aws.kinesis.worker.Pogo(bar))"::equals)) {
            System.err.println("Verfifed listenPogoRecord OK");
            passedEvents++;
        }

        System.err.println("Passed " + passedEvents + " event checks of " + expected);

        return passedEvents == expected;
    }

}
