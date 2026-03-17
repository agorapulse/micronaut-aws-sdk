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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * Event published after processing a batch of Kinesis records.
 * <p>
 * This event can be used to publish custom metrics (e.g., to Datadog) based on
 * the Kinesis consumer state, particularly the {@link #getMillisBehindLatest()} value
 * which indicates how far behind the consumer is from the stream tip.
 * <p>
 * Example usage:
 * <pre>{@code
 * @Singleton
 * public class KinesisMetricsPublisher {
 *     
 *     private final MeterRegistry meterRegistry;
 *     
 *     public KinesisMetricsPublisher(MeterRegistry meterRegistry) {
 *         this.meterRegistry = meterRegistry;
 *     }
 *     
 *     @EventListener
 *     void onProcessRecords(ProcessRecordsEvent event) {
 *         if (event.getMillisBehindLatest() != null) {
 *             meterRegistry.gauge("kinesis.consumer.millisBehindLatest",
 *                 Tags.of("stream", event.getStream(), "shard", event.getShardId()),
 *                 event.getMillisBehindLatest());
 *         }
 *     }
 * }
 * }</pre>
 */
public class ProcessRecordsEvent {

    private final String stream;
    private final String shardId;
    private final Long millisBehindLatest;
    private final int recordCount;

    public ProcessRecordsEvent(
        @NonNull String stream,
        @NonNull String shardId,
        @Nullable Long millisBehindLatest,
        int recordCount
    ) {
        this.stream = stream;
        this.shardId = shardId;
        this.millisBehindLatest = millisBehindLatest;
        this.recordCount = recordCount;
    }

    /**
     * @return the name of the Kinesis stream
     */
    @NonNull
    public String getStream() {
        return stream;
    }

    /**
     * @return the shard ID being processed
     */
    @NonNull
    public String getShardId() {
        return shardId;
    }

    /**
     * Returns the number of milliseconds the consumer is behind the tip of the stream.
     * <p>
     * This value is useful for monitoring consumer lag. A high value indicates the consumer
     * is falling behind and may need scaling or optimization.
     * <p>
     * Note: This value may be null if not provided by the Kinesis Client Library.
     *
     * @return milliseconds behind the latest record in the stream, or null if not available
     */
    @Nullable
    public Long getMillisBehindLatest() {
        return millisBehindLatest;
    }

    /**
     * @return the number of records in this batch
     */
    public int getRecordCount() {
        return recordCount;
    }

    @Override
    public String toString() {
        return "ProcessRecordsEvent{" +
            "stream='" + stream + '\'' +
            ", shardId='" + shardId + '\'' +
            ", millisBehindLatest=" + millisBehindLatest +
            ", recordCount=" + recordCount +
            '}';
    }

}
