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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.BiConsumer;

@Singleton
@Requires(
    property = "aws.kinesis"
)
public class DefaultKinesisWorkerFactory implements KinesisWorkerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKinesisWorkerFactory.class);

    private static final KinesisWorker NOOP = new KinesisWorker() {
        @Override
        public void start() {
            // do nothing
        }

        @Override
        public void shutdown() {
            // do nothing
        }

        @Override
        public void addConsumer(BiConsumer<String, KinesisClientRecord> next) {
            // do nothing
        }
    };

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Optional<DynamoDbAsyncClient> amazonDynamoDB;
    private final Optional<KinesisAsyncClient> kinesis;
    private final Optional<CloudWatchAsyncClient> cloudWatch;

    public DefaultKinesisWorkerFactory(ApplicationEventPublisher applicationEventPublisher, Optional<DynamoDbAsyncClient> amazonDynamoDB, Optional<KinesisAsyncClient> kinesis, Optional<CloudWatchAsyncClient> cloudWatch) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.amazonDynamoDB = amazonDynamoDB;
        this.kinesis = kinesis;
        this.cloudWatch = cloudWatch;
    }

    @Override
    public KinesisWorker create(KinesisClientConfiguration kinesisConfiguration) {
        if (kinesisConfiguration == null) {
            return NOOP;
        }

        if (StringUtils.isEmpty(kinesisConfiguration.getApplicationName())) {
            LOGGER.error("Cannot setup listener Kinesis listener, application name is missing. Please, set 'aws.kinesis.application.name' property");
            return NOOP;
        }

        return new DefaultKinesisWorker(kinesisConfiguration, applicationEventPublisher, amazonDynamoDB, kinesis, cloudWatch);
    }

}
