/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.kinesis.worker

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j

import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.util.function.BiConsumer

/**
 * Default record processor used by the Kinesis listeners.
 */
@Slf4j
@CompileStatic
@PackageScope
class DefaultRecordProcessor implements IRecordProcessor {

    // Backoff and retry settings
    private static final long BACKOFF_TIME_IN_MILLIS = 3000L
    private static final int NUM_RETRIES = 10
    // Checkpoint about once a minute
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L

    static IRecordProcessor create(BiConsumer<String, Record> consumer) {
        return new DefaultRecordProcessor(consumer)
    }

    private long nextCheckpointTimeInMillis

    private final CharsetDecoder decoder = Charset.forName('UTF-8').newDecoder()

    String shardId = ''

    private final BiConsumer<String, Record> processor

    @Override
    void initialize(String shardId) {
        this.shardId = shardId
        log.debug "[${shardId}] Initializing , thread = ${Thread.currentThread().id}, ${Thread.currentThread().name}"
    }

    @Override
    void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer)  {
        log.debug "[${shardId}] Processing: ${records.size()}, thread = ${Thread.currentThread().id}, ${Thread.currentThread().name}"

        // Process records and perform all exception handling.
        processRecordsWithRetries(records)

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(checkpointer)
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS
        }
    }

    @Override
    void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason shutdownReason) {
        log.debug "[${shardId}] Shutting down: ${shutdownReason}, thread = ${Thread.currentThread().id}, ${Thread.currentThread().name}"
        if (shutdownReason == ShutdownReason.TERMINATE) {
            checkpoint(checkpointer)
        }
    }

    private DefaultRecordProcessor(BiConsumer<String, Record> processor) {
        this.processor = processor
    }

    /**
     * Process records performing retries as needed. Skip "poison pill" records.
     *
     * @param records Data records to be processed.
     */
    @SuppressWarnings('CatchThrowable')
    private void processRecordsWithRetries(List<Record> records) {
        records.each { Record record ->
            boolean processedSuccessfully = false
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    // Process single record
                    processSingleRecord(record)

                    processedSuccessfully = true
                    break
                } catch (Throwable t) {
                    log.warn("[${shardId}] Caught throwable while processing record ${record}", t)
                }

                // backoff if we encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS)
                } catch (InterruptedException e) {
                    log.debug "[${shardId}] Interrupted sleep", e
                }
            }

            if (!processedSuccessfully) {
                log.error "[${shardId}] Couldn't process record ${record}. Skipping the record."
            }
        }
    }

    /**
     * Process a single record.
     *
     * @param record The record to be processed.
     */
    private void processSingleRecord(Record record) {
        String data
        try {
            // For this app, we interpret the payload as UTF-8 chars.
            data = decoder.decode(record.data).toString()

            // Process record (method to be overriden with custom code)
            log.debug "[${shardId}] ${record.sequenceNumber}, ${record.partitionKey}, processRecordData not implemented"
            processor.accept(data, record)
        } catch (CharacterCodingException e) {
            log.error "[${shardId}] Malformed data: ${data}", e
        }
    }

    /** Checkpoint with retries.
     * @param checkpointer
     */
    protected void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        log.info "[${shardId}] Checkpointing shard"
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
                checkpointer.checkpoint()
                break
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                log.info "[${shardId}] Caught shutdown exception, skipping checkpoint.", se
                break
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    log.error "[${shardId}] Checkpoint failed after ${i + 1} attempts.", e
                    break
                } else {
                    log.info "[${shardId}] Transient issue when checkpointing - attempt ${i + 1} of $NUM_RETRIES", e
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                log.error "[${shardId}] Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e
                break
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS)
            } catch (InterruptedException e) {
                log.debug "[${shardId}] Interrupted sleep", e
            }
        }
    }

}
