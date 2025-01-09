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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Default record processor used by the Kinesis listeners.
 */
class DefaultRecordProcessor implements ShardRecordProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRecordProcessor.class);

    // Backoff and retry settings
    private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 10;

    // Checkpoint about once a minute
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L;

    static ShardRecordProcessor create(BiConsumer<String, KinesisClientRecord> consumer) {
        return new DefaultRecordProcessor(consumer);
    }

    private long nextCheckpointTimeInMillis;
    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
    private final BiConsumer<String, KinesisClientRecord> processor;

    private String shardId = "";

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        LOGGER.debug("[{}] Lost lease, so terminating.", shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        try {
            LOGGER.debug("[{}] Reached shard end checkpointing.", shardId);
            shardEndedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            LOGGER.error("Exception while checkpointing at shard end. Giving up.", e);
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        try {
            LOGGER.debug("[{}] Scheduler is shutting down, checkpointing.", shardId);
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            LOGGER.error("Exception while checkpointing at requested shutdown. Giving up.", e);
        }
    }

    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.shardId();

        Thread thread = Thread.currentThread();
        LOGGER.debug("[{}] Initializing: thread = {}: {}, sequence = {}", shardId, thread.getId(), thread.getName(), initializationInput.extendedSequenceNumber());
    }

    public void processRecords(ProcessRecordsInput input) {
        Thread thread = Thread.currentThread();
        LOGGER.debug("[{}] Processing: {} records, thread = {}: {}", shardId, input.records().size(), thread.getId(), thread.getName());

        // Process records and perform all exception handling.
        processRecordsWithRetries(input.records());

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(input.checkpointer());
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }

    private DefaultRecordProcessor(BiConsumer<String, KinesisClientRecord> processor) {
        this.processor = processor;
    }

    /**
     * Process records performing retries as needed. Skip "poison pill" records.
     *
     * @param records Data records to be processed.
     */
    private void processRecordsWithRetries(List<KinesisClientRecord> records) {
        records.forEach(record -> {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    // Process single record
                    processSingleRecord(record);

                    processedSuccessfully = true;
                    break;
                } catch (Throwable t) {
                    LOGGER.warn("[" + shardId + "] Caught throwable while processing record " + record, t);
                }

                // backoff if we encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
                    LOGGER.debug("[{}] Interrupted sleep", shardId, e);
                }
            }

            if (!processedSuccessfully) {
                LOGGER.error("[{}] Couldn't process record {}. Skipping the record.", shardId, record);
            }
        });
    }

    /**
     * Process a single record.
     *
     * @param record The record to be processed.
     */
    private void processSingleRecord(KinesisClientRecord record) {
        try {
            // For this app, we interpret the payload as UTF-8 chars.
            processor.accept(decoder.decode(record.data()).toString(), record);
        } catch (CharacterCodingException e) {
            LOGGER.error("[" + shardId + "] Malformed data: " + record.data(), e);
        }
    }

    /**
     * Checkpoint with retries.
     *
     * @param checkpointer
     */
    protected void checkpoint(RecordProcessorCheckpointer checkpointer) {
        LOGGER.debug("[{}] Checkpointing shard", shardId);
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint();
                break;
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOGGER.debug("[" + shardId + "] Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOGGER.error("[" + shardId + "] Checkpoint failed after " + (i + 1) + " attempts.", e);
                    break;
                } else {
                    LOGGER.debug("[" + shardId + "] Transient issue when checkpointing - attempt " + (i + 1) + " of $NUM_RETRIES", e);
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOGGER.error("[" + shardId + "] Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                LOGGER.debug("[" + shardId + "] Interrupted sleep", e);
            }
        }
    }

}
