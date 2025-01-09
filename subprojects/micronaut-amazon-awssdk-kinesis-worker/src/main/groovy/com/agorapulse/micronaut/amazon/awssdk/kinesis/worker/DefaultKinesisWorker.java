/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
import software.amazon.kinesis.coordinator.WorkerStateChangeListener;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

class DefaultKinesisWorker implements KinesisWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKinesisWorker.class);

    private final KinesisClientConfiguration configuration;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Optional<DynamoDbAsyncClient> amazonDynamoDB;
    private final Optional<KinesisAsyncClient> amazonKinesis;
    private final Optional<CloudWatchAsyncClient> amazonCloudWatch;
    private final List<BiConsumer<String, KinesisClientRecord>> consumers = new CopyOnWriteArrayList<>();

    private Scheduler scheduler;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    DefaultKinesisWorker(
        KinesisClientConfiguration configuration,
        ApplicationEventPublisher applicationEventPublisher,
        Optional<DynamoDbAsyncClient> amazonDynamoDB,
        Optional<KinesisAsyncClient> amazonKinesis,
        Optional<CloudWatchAsyncClient> amazonCloudWatch
    ) {
        this.configuration = configuration;
        this.applicationEventPublisher = applicationEventPublisher;
        this.amazonDynamoDB = amazonDynamoDB;
        this.amazonKinesis = amazonKinesis;
        this.amazonCloudWatch = amazonCloudWatch;
    }

    @Override
    public void start() {
        ShardRecordProcessorFactory recordProcessorFactory = DefaultRecordProcessorFactory.create((string, record) ->
            consumers.forEach(c -> {
                try {
                    c.accept(string, record);
                } catch (Exception e) {
                    LOGGER.error("Exception processing Kinesis record " + record + " decoded as " + string, e);
                }
            })
        );

        ConfigsBuilder configsBuilder = configuration.getConfigsBuilder(
            amazonDynamoDB,
            amazonKinesis,
            amazonCloudWatch,
            recordProcessorFactory
        );

        WorkerStateChangeListener workerStateChangeListener = s -> applicationEventPublisher.publishEvent(new WorkerStateEvent(s, configuration.getStream()));

        PollingConfig pollingConfig = new PollingConfig(configuration.getStream(), configsBuilder.kinesisClient());

        scheduler = new Scheduler(
            configsBuilder.checkpointConfig(),
            configsBuilder.coordinatorConfig().workerStateChangeListener(workerStateChangeListener),
            configsBuilder.leaseManagementConfig(),
            configsBuilder.lifecycleConfig(),
            configsBuilder.metricsConfig(),
            configsBuilder.processorConfig(),
            configsBuilder.retrievalConfig().retrievalSpecificConfig(pollingConfig)
        );

        try {
            LOGGER.debug("Starting Kinesis worker for {}", configuration.getStream());
            executorService.submit(scheduler);
        } catch (Exception t) {
            LOGGER.error("Caught throwable while processing Kinesis data.", t);
        }
    }

    @Override
    public void shutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (Exception e) {
                LOGGER.warn("Exception shutting down worker", e);
            }
        }
        try {
            executorService.shutdown();
        } catch (Exception e) {
            LOGGER.warn("Exception shutting down worker's executor service", e);
        }
    }

    @Override
    public void addConsumer(BiConsumer<String, KinesisClientRecord> next) {
        consumers.add(next);
    }

}
