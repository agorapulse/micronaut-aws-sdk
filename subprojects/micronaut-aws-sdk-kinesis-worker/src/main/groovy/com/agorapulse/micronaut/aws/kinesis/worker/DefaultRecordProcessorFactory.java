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
package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.model.Record;

import java.util.function.BiConsumer;

class DefaultRecordProcessorFactory implements IRecordProcessorFactory {

    static IRecordProcessorFactory create(BiConsumer<String, Record> consumer) {
        return new DefaultRecordProcessorFactory(consumer);
    }

    private DefaultRecordProcessorFactory(BiConsumer<String, Record> consumer) {
        this.consumer = consumer;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return DefaultRecordProcessor.create(consumer);
    }

    private BiConsumer<String, Record> consumer;

}
