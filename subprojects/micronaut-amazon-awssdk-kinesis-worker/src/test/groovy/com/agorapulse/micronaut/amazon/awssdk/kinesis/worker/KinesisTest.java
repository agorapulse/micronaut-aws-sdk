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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisService;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

// tag::testcontainers-header[]
@MicronautTest
@Property(name = "localstack.services", value = "cloudwatch,dynamodb,kinesis")
@Property(name = "aws.kinesis.application.name", value = KinesisTest.APP_NAME)
@Property(name = "aws.kinesis.worker.id", value = "abcdef")
@Property(name = "aws.kinesis.stream", value = KinesisTest.TEST_STREAM)
@Property(name = "aws.kinesis.listener.stream", value = KinesisTest.TEST_STREAM)
@Property(name = "aws.kinesis.listener.failover-time-millis", value = "1000")
@Property(name = "aws.kinesis.listener.shard-sync-interval-millis", value = "1000")
@Property(name = "aws.kinesis.listener.idle-time-between-reads-in-millis", value = "1000")
@Property(name = "aws.kinesis.listener.parent-shard-poll-interval-millis", value = "1000")
@Property(name = "aws.kinesis.listener.timeout-in-seconds", value = "1000")
@Property(name = "aws.kinesis.listener.retry-get-records-in-seconds", value = "1000")
@Property(name = "aws.kinesis.listener.metrics-level", value = "NONE")
public class KinesisTest {
// end::testcontainers-header[]

    public static final String APP_NAME = "TestApp";
    public static final String TEST_STREAM = "MyStream";

    @Inject KinesisService service;
    @Inject KinesisListenerTester tester;
    @Inject DefaultClient client;
    @Inject WorkerStateListener listener;

    // tag::testcontainers-test[]
    @Test
    public void testJavaService() throws InterruptedException {
        service.createStream();
        service.waitForActive();

        waitForWorkerReady(1200, 100);
        Disposable subscription = publishEventsAsync(tester, client);
        waitForRecievedMessages(tester, 300, 100);

        subscription.dispose();

        Assertions.assertTrue(allTestEventsReceived(tester));
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

        if (tester.getExecutions().stream().anyMatch("EXECUTED: listenPogoRecord(com.agorapulse.micronaut.amazon.awssdk.kinesis.worker.Pogo(bar))"::equals)) {
            System.err.println("Verfifed listenPogoRecord OK");
            passedEvents++;
        }

        System.err.println("Passed " + passedEvents + " event checks of " + expected);

        return passedEvents == expected;
    }

}
