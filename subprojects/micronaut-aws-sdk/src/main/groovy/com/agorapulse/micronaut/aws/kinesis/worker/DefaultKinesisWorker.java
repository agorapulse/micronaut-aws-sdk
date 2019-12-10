/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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

import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisListener;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.Record;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

class DefaultKinesisWorker implements KinesisWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisListener.class);

    DefaultKinesisWorker(
        KinesisClientLibConfiguration configuration,
        ApplicationEventPublisher applicationEventPublisher,
        Optional<AmazonDynamoDB> amazonDynamoDB,
        Optional<AmazonKinesis> amazonKinesis,
        Optional<AmazonCloudWatch> amazonCloudWatch
    ) {
        this.configuration = configuration;
        this.applicationEventPublisher = applicationEventPublisher;
        this.amazonDynamoDB = amazonDynamoDB;
        this.amazonKinesis = amazonKinesis;
        this.amazonCloudWatch = amazonCloudWatch;
    }

    @Override
    public void start() {
        Worker.Builder builder = new Worker.Builder().config(configuration);

        if (StringUtils.isEmpty(configuration.getDynamoDBEndpoint())) {
            amazonDynamoDB.ifPresent(builder::dynamoDBClient);
        }

        if (StringUtils.isEmpty(configuration.getKinesisEndpoint())) {
            amazonKinesis.ifPresent(builder::kinesisClient);
        }

        amazonCloudWatch.ifPresent(builder::cloudWatchClient);

        builder.recordProcessorFactory(DefaultRecordProcessorFactory.create((string, record) ->
            consumers.forEach(c -> {
                try {
                    c.accept(string, record);
                } catch (Exception e) {
                    LOGGER.error("Exception processing Kinesis record " + record + " decoded as " + string, e);
                }
            }))
        );

        builder.workerStateChangeListener(s -> applicationEventPublisher.publishEvent(new WorkerStateEvent(s, configuration.getStreamName())));

        try {
            LOGGER.info("Starting Kinesis worker for {}", configuration.getStreamName());
            worker = builder.build();
            executorService.execute(worker);
        } catch (Exception t) {
            LOGGER.error("Caught throwable while processing Kinesis data.", t);
        }
    }

    @Override
    public void shutdown() {
        if (worker != null) {
            try {
                worker.shutdown();
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
    public void addConsumer(BiConsumer<String, Record> next) {
        consumers.add(next);
    }

    private final KinesisClientLibConfiguration configuration;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Optional<AmazonDynamoDB> amazonDynamoDB;
    private final Optional<AmazonKinesis> amazonKinesis;
    private final Optional<AmazonCloudWatch> amazonCloudWatch;
    private final List<BiConsumer<String, Record>> consumers = new CopyOnWriteArrayList<>();
    private Worker worker;
}
