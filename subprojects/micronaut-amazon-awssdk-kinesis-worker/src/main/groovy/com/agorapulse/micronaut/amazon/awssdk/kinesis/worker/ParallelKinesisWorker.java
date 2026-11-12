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

import io.micronaut.context.event.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Same wiring as {@link DefaultKinesisWorker}, with {@link ParallelRecordProcessor} and a graceful shutdown that checkpoints.
 * Used when {@link KinesisClientConfiguration#getConcurrency()} is greater than zero.
 */
class ParallelKinesisWorker implements KinesisWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelKinesisWorker.class);

    private static final long GRACEFUL_SHUTDOWN_TIMEOUT_MILLIS = 20_000L;

    private final KinesisClientConfiguration configuration;
    private final ApplicationEventPublisher<WorkerStateEvent> workerStateEventPublisher;
    private final ApplicationEventPublisher<ProcessRecordsEvent> processRecordsEventPublisher;
    private final Optional<DynamoDbAsyncClient> amazonDynamoDB;
    private final Optional<KinesisAsyncClient> amazonKinesis;
    private final Optional<CloudWatchAsyncClient> amazonCloudWatch;

    private final List<BiConsumer<String, KinesisClientRecord>> consumers = new CopyOnWriteArrayList<>();
    private final ExecutorService schedulerExecutor = Executors.newSingleThreadExecutor();
    // Owned rather than Micronaut's shared virtual executor, which other beans shut down on destroy
    private final ExecutorService recordExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
        .name("kinesis-record-", 0)
        .uncaughtExceptionHandler((thread, error) -> LOGGER.error("Unexpected error while processing a Kinesis record", error))
        .factory());
    private Scheduler scheduler;

    ParallelKinesisWorker(
        KinesisClientConfiguration configuration,
        ApplicationEventPublisher<WorkerStateEvent> workerStateEventPublisher,
        ApplicationEventPublisher<ProcessRecordsEvent> processRecordsEventPublisher,
        Optional<DynamoDbAsyncClient> amazonDynamoDB,
        Optional<KinesisAsyncClient> amazonKinesis,
        Optional<CloudWatchAsyncClient> amazonCloudWatch
    ) {
        this.configuration = configuration;
        this.workerStateEventPublisher = workerStateEventPublisher;
        this.processRecordsEventPublisher = processRecordsEventPublisher;
        this.amazonDynamoDB = amazonDynamoDB;
        this.amazonKinesis = amazonKinesis;
        this.amazonCloudWatch = amazonCloudWatch;
    }

    @Override
    public void start() {
        ParallelRecordProcessor.Settings settings = new ParallelRecordProcessor.Settings(
            configuration.getConcurrency(),
            configuration.getMaxAttempts(),
            configuration.isOrderedByPartitionKey()
        );
        Semaphore slots = new Semaphore(settings.concurrency(), true);
        String stream = configuration.getStream();
        ConfigsBuilder configsBuilder = configuration.getConfigsBuilder(
            amazonDynamoDB,
            amazonKinesis,
            amazonCloudWatch,
            () -> new ParallelRecordProcessor(this::dispatch, recordExecutor, slots, settings, processRecordsEventPublisher, stream, ParallelRecordProcessor.Timings.DEFAULT)
        );
        PollingConfig pollingConfig = new PollingConfig(stream, configsBuilder.kinesisClient());
        scheduler = new Scheduler(
            configsBuilder.checkpointConfig(),
            configsBuilder.coordinatorConfig().workerStateChangeListener(
                state -> workerStateEventPublisher.publishEvent(new WorkerStateEvent(state, stream))),
            configsBuilder.leaseManagementConfig(),
            configsBuilder.lifecycleConfig(),
            configsBuilder.metricsConfig(),
            configsBuilder.processorConfig(),
            configsBuilder.retrievalConfig().retrievalSpecificConfig(pollingConfig)
        );
        LOGGER.info("Starting parallel Kinesis worker for {} with {}", stream, settings);
        schedulerExecutor.execute(scheduler);
    }

    @Override
    public void shutdown() {
        if (scheduler != null) {
            try {
                scheduler.startGracefulShutdown().get(GRACEFUL_SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.warn("Graceful shutdown of the Kinesis worker for {} did not complete", configuration.getStream(), e);
                scheduler.shutdown();
            }
        }
        schedulerExecutor.shutdown();
        recordExecutor.shutdown();
    }

    @Override
    public void addConsumer(BiConsumer<String, KinesisClientRecord> next) {
        consumers.add(next);
    }

    private void dispatch(String data, KinesisClientRecord kinesisRecord) {
        consumers.forEach(consumer -> consumer.accept(data, kinesisRecord));
    }
}
