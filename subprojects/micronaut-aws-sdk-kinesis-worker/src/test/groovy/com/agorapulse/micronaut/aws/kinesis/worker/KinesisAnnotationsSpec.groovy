/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.aws.kinesis.worker

import com.agorapulse.micronaut.aws.kinesis.KinesisService
import io.micronaut.context.annotation.Property
import io.micronaut.test.annotation.MicronautTest
import reactor.core.Disposable
import reactor.core.publisher.Flux
import spock.lang.Requires
import spock.lang.Specification

import javax.inject.Inject
import java.time.Duration

/**
 * Tests for Kinesis related annotations - client and listener.
 */

@SuppressWarnings('ClassStartsWithBlankLine')
// TODO remove once fixed
@Requires({ System.getenv('CI') != 'true' })
// tag::testcontainers-header[]
@MicronautTest
@Property(name = 'localstack.services', value = 'cloudwatch,dynamodb,kinesis')
@Property(name = 'aws.kinesis.application.name', value = APP_NAME)
@Property(name = 'aws.kinesis.stream', value = TEST_STREAM)
@Property(name = 'aws.kinesis.listener.stream', value = TEST_STREAM)
@Property(name = 'aws.kinesis.listener.failover-time-millis', value = '1000')
@Property(name = 'aws.kinesis.listener.shard-sync-interval-millis', value = '1000')
@Property(name = 'aws.kinesis.listener.idle-time-between-reads-in-millis', value = '1000')
@Property(name = 'aws.kinesis.listener.parent-shard-poll-interval-millis', value = '1000')
@Property(name = 'aws.kinesis.listener.timeout-in-seconds', value = '1000')
@Property(name = 'aws.kinesis.listener.retry-get-records-in-seconds', value = '1000')
@Property(name = 'aws.kinesis.listener.metrics-level', value = 'NONE')
class KinesisAnnotationsSpec extends Specification {
// end::testcontainers-header[]

    private static final String TEST_STREAM = 'TestStream'
    private static final String APP_NAME = 'AppName'

    @Inject KinesisService service
    @Inject KinesisListenerTester tester
    @Inject DefaultClient client
    @Inject WorkerStateListener listener

    // tag::testcontainers-test[]
    void 'kinesis listener is executed'() {
        when:
            service.createStream()
            service.waitForActive()

            waitForWorkerReady(1200, 100)

            Disposable subscription = publishEventAsync(tester, client)

            waitForReceivedMessages(tester, 300, 100)

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
        return Flux
            .interval(Duration.ofMillis(100))
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

        if (tester.executions.any { it == 'EXECUTED: listenPogoRecord(com.agorapulse.micronaut.aws.kinesis.worker.Pogo(bar))' }) {
            System.err.println('Verfifed listenPogoRecord OK')
            passedEvents++
        }

        System.err.println("Passed $passedEvents event checks of $EXPECTED")

        return passedEvents == EXPECTED
    }

    @SuppressWarnings('SystemErrPrint')
    private void waitForWorkerReady(int retries, int waitMillis) throws InterruptedException {
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
