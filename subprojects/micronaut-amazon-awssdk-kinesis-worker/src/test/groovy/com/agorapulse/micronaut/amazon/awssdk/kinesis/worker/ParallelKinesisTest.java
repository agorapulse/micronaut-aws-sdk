/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

/**
 * Runs the listener scenario of {@link KinesisTest} with the parallel processing enabled.
 */
@MicronautTest
@Property(name = "localstack.services", value = "cloudwatch,dynamodb,kinesis")
@Property(name = "aws.kinesis.application.name", value = KinesisTest.APP_NAME)
@Property(name = "aws.kinesis.worker.id", value = "abcdef")
@Property(name = "aws.kinesis.stream", value = KinesisTest.TEST_STREAM)
@Property(name = "aws.kinesis.listener.stream", value = KinesisTest.TEST_STREAM)
@Property(name = "aws.kinesis.listener.concurrency", value = "4")
@Property(name = "aws.kinesis.listener.max-attempts", value = "3")
@Property(name = "aws.kinesis.listener.failover-time-millis", value = "1000")
@Property(name = "aws.kinesis.listener.shard-sync-interval-millis", value = "1000")
@Property(name = "aws.kinesis.listener.parent-shard-poll-interval-millis", value = "1000")
public class ParallelKinesisTest extends KinesisTest {

    @Inject KinesisClientConfiguration configuration;
    @Inject KinesisWorkerFactory workerFactory;

    @Test
    public void testParallelWorkerIsSelected() {
        Assertions.assertEquals(4, configuration.getConcurrency());
        Assertions.assertEquals(3, configuration.getMaxAttempts());
        Assertions.assertTrue(configuration.isOrderedByPartitionKey());
        Assertions.assertInstanceOf(ParallelKinesisWorker.class, workerFactory.create(configuration));
    }

}
