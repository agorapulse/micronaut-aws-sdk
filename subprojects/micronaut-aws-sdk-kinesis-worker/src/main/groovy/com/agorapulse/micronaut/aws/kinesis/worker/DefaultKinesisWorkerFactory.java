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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;

import javax.inject.Singleton;

@Singleton
@Requires(
    classes = KinesisClientLibConfiguration.class,
    property = "aws.kinesis"
)
public class DefaultKinesisWorkerFactory implements KinesisWorkerFactory {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AmazonDynamoDB amazonDynamoDB;
    private final AmazonKinesis kinesis;
    private final AmazonCloudWatch cloudWatch;

    public DefaultKinesisWorkerFactory(ApplicationEventPublisher applicationEventPublisher, AmazonDynamoDB amazonDynamoDB, AmazonKinesis kinesis, AmazonCloudWatch cloudWatch) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.amazonDynamoDB = amazonDynamoDB;
        this.kinesis = kinesis;
        this.cloudWatch = cloudWatch;
    }

    @Override
    public KinesisWorker create(KinesisClientLibConfiguration kinesisConfiguration) {
        if (kinesisConfiguration == null) {
            throw new IllegalStateException("Cannot setup listener Kinesis listener, configuration is missing");
        }

        if (StringUtils.isEmpty(kinesisConfiguration.getApplicationName())) {
            throw new IllegalStateException("Cannot setup listener Kinesis listener, application name is missing. Please, set 'aws.kinesis.application.name' property");
        }

        return new DefaultKinesisWorker(kinesisConfiguration, applicationEventPublisher, amazonDynamoDB, kinesis, cloudWatch);
    }
}
