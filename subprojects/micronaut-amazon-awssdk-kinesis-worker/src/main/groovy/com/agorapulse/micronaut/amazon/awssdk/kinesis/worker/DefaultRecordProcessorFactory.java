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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.util.function.BiConsumer;

class DefaultRecordProcessorFactory implements ShardRecordProcessorFactory {

    private final BiConsumer<String, KinesisClientRecord> consumer;

    static ShardRecordProcessorFactory create(BiConsumer<String, KinesisClientRecord> consumer) {
        return new DefaultRecordProcessorFactory(consumer);
    }

    private DefaultRecordProcessorFactory(BiConsumer<String, KinesisClientRecord> consumer) {
        this.consumer = consumer;
    }

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return DefaultRecordProcessor.create(consumer);
    }


}
