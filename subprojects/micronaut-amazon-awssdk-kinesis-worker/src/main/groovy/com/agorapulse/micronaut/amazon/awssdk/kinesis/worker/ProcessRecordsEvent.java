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

/**
 * Event published after each batch of records is processed by the Kinesis worker.
 *
 * Contains batch-level metadata from {@link software.amazon.kinesis.lifecycle.events.ProcessRecordsInput}.
 */
public class ProcessRecordsEvent {

    private final String stream;
    private final String shardId;
    private final Long millisBehindLatest;
    private final int recordCount;

    public ProcessRecordsEvent(String stream, String shardId, Long millisBehindLatest, int recordCount) {
        this.stream = stream;
        this.shardId = shardId;
        this.millisBehindLatest = millisBehindLatest;
        this.recordCount = recordCount;
    }

    public String getStream() {
        return stream;
    }

    public String getShardId() {
        return shardId;
    }

    public Long getMillisBehindLatest() {
        return millisBehindLatest;
    }

    public int getRecordCount() {
        return recordCount;
    }

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "ProcessRecordsEvent{" +
            "stream='" + stream + '\'' +
            ", shardId='" + shardId + '\'' +
            ", millisBehindLatest=" + millisBehindLatest +
            ", recordCount=" + recordCount +
            '}';
    }
    // CHECKSTYLE:ON
}