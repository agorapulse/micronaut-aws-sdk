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

import io.micronaut.context.event.ApplicationEventPublisher;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;

/**
 * Processes each record on a virtual thread, bounded by the slots shared across the task's shards, and checkpoints
 * behind the oldest record still being processed, so a checkpoint never covers an unfinished record.
 * Unless the order is disabled, records sharing a partition key are processed one at a time, in the order of the shard.
 */
class ParallelRecordProcessor implements ShardRecordProcessor {

    /**
     * Minimum delay between two checkpoints of a shard, and how long a shutdown waits for the records in flight.
     */
    record Timings(Duration checkpointInterval, Duration shutdownDrainTimeout) {
        static final Timings DEFAULT = new Timings(Duration.ofSeconds(60), Duration.ofSeconds(15));
    }

    /**
     * Records the worker processes at once, attempts a record gets, and whether the order of a partition key is kept.
     */
    record Settings(int concurrency, int maxAttempts, boolean orderedByPartitionKey) { }

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelRecordProcessor.class);

    private static final Duration SHARD_END_DRAIN_TIMEOUT = Duration.ofMinutes(5);
    private static final long RETRY_BACKOFF_MILLIS = 200L;
    private static final long DRAIN_POLL_MILLIS = 20L;

    private final BiConsumer<String, KinesisClientRecord> consumer;
    private final ExecutorService executor;
    private final Semaphore slots;
    private final Semaphore pending;
    private final int maxAttempts;
    private final boolean orderedByPartitionKey;
    private final ApplicationEventPublisher<ProcessRecordsEvent> eventPublisher;
    private final String stream;
    private final Timings timings;

    private final ConcurrentLinkedQueue<Tracked> inFlight = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<Boolean>> lastByPartitionKey = new ConcurrentHashMap<>();
    private String shardId = "";
    private long nextCheckpointTimeInMillis;
    private KinesisClientRecord lastCompleted;
    private KinesisClientRecord lastCheckpointed;
    private volatile boolean checkpointingSuspended;

    ParallelRecordProcessor(
        BiConsumer<String, KinesisClientRecord> consumer,
        ExecutorService executor,
        Semaphore slots,
        Settings settings,
        ApplicationEventPublisher<ProcessRecordsEvent> eventPublisher,
        String stream,
        Timings timings
    ) {
        this.consumer = consumer;
        this.executor = executor;
        this.slots = slots;
        this.pending = new Semaphore(settings.concurrency());
        this.maxAttempts = settings.maxAttempts();
        this.orderedByPartitionKey = settings.orderedByPartitionKey();
        this.eventPublisher = eventPublisher;
        this.stream = stream;
        this.timings = timings;
    }

    @Override
    public void initialize(InitializationInput input) {
        shardId = input.shardId();
        LOGGER.info("[{}] Initialized at sequence {}", shardId, input.extendedSequenceNumber());
    }

    // Blocks the shard thread while the shard has as many unfinished records as the concurrency, so the KCL stops reading it.
    @Override
    public void processRecords(ProcessRecordsInput input) {
        for (KinesisClientRecord kinesisRecord : input.records()) {
            if (!submit(kinesisRecord)) {
                suspendCheckpointing("record " + kinesisRecord.sequenceNumber() + " could not be submitted");
                return;
            }
        }
        if (!input.records().isEmpty()) {
            eventPublisher.publishEventAsync(new ProcessRecordsEvent(stream, shardId, input.millisBehindLatest(), input.records().size()));
        }

        if (System.currentTimeMillis() >= nextCheckpointTimeInMillis) {
            checkpointCompleted(input.checkpointer());
            nextCheckpointTimeInMillis = System.currentTimeMillis() + timings.checkpointInterval().toMillis();
        }
    }

    @Override
    public void leaseLost(LeaseLostInput input) {
        LOGGER.info("[{}] Lease lost with {} records in flight", shardId, inFlight.size());
    }

    // Without the shard-end checkpoint the KCL fails the shutdown and calls this again later.
    @Override
    public void shardEnded(ShardEndedInput input) {
        if (checkpointingSuspended || !drain(SHARD_END_DRAIN_TIMEOUT)) {
            LOGGER.error("[{}] Shard ended with {} records still in flight or checkpointing suspended, leaving the shard end for later", shardId, inFlight.size());
            return;
        }
        try {
            input.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            LOGGER.error("[{}] Cannot checkpoint at shard end", shardId, e);
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput input) {
        if (!drain(timings.shutdownDrainTimeout())) {
            LOGGER.warn("[{}] Shutting down with {} records still in flight, they will be processed again", shardId, inFlight.size());
        }
        checkpointCompleted(input.checkpointer());
    }

    // An Error escaping the handler still marks the record done, otherwise it would hold the checkpoint forever.
    private boolean submit(KinesisClientRecord kinesisRecord) {
        try {
            pending.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        Tracked tracked = new Tracked(kinesisRecord);
        inFlight.add(tracked);
        String partitionKey = Objects.requireNonNullElse(kinesisRecord.partitionKey(), "");
        CompletableFuture<Boolean> previous = orderedByPartitionKey ? lastByPartitionKey.get(partitionKey) : null;
        CompletableFuture<Boolean> current = new CompletableFuture<>();
        if (orderedByPartitionKey) {
            lastByPartitionKey.put(partitionKey, current);
        }
        try {
            executor.execute(() -> {
                boolean interrupted = false;
                try {
                    interrupted = !processInOrder(previous, kinesisRecord);
                } finally {
                    tracked.done = !interrupted;
                    lastByPartitionKey.remove(partitionKey, current);
                    current.complete(!interrupted);
                    pending.release();
                }
            });
        } catch (RejectedExecutionException e) {
            inFlight.remove(tracked);
            lastByPartitionKey.remove(partitionKey, current);
            current.complete(false);
            pending.release();
            return false;
        }
        return true;
    }

    // Waits for the previous record of the partition key before taking a slot, so a busy key only ever holds one slot.
    private boolean processInOrder(CompletableFuture<Boolean> previous, KinesisClientRecord kinesisRecord) {
        try {
            if (previous != null && !previous.get()) {
                return false;
            }
            slots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            suspendCheckpointing("record " + kinesisRecord.sequenceNumber() + " was interrupted before being processed");
            return false;
        } catch (ExecutionException e) {
            return false;
        }
        try {
            return processWithRetries(kinesisRecord);
        } finally {
            slots.release();
        }
    }

    // The KCL skips the rest of a batch whose records could not be submitted, so no later checkpoint may pass them.
    private void suspendCheckpointing(String reason) {
        checkpointingSuspended = true;
        inFlight.clear();
        LOGGER.error("[{}] Checkpointing suspended until the lease is taken over: {}", shardId, reason);
    }

    private boolean processWithRetries(KinesisClientRecord kinesisRecord) {
        String data = StandardCharsets.UTF_8.decode(kinesisRecord.data()).toString();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                consumer.accept(data, kinesisRecord);
                return true;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    LOGGER.error("[{}] Dropping record {} after {} attempt(s)", shardId, kinesisRecord.sequenceNumber(), attempt, e);
                } else {
                    LOGGER.warn("[{}] Retrying record {} after attempt {}", shardId, kinesisRecord.sequenceNumber(), attempt, e);
                    if (!backoff()) {
                        suspendCheckpointing("record " + kinesisRecord.sequenceNumber() + " was interrupted before its retry");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean backoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Only the shard thread polls the queue, the processing threads only flag their record as done.
    private void checkpointCompleted(RecordProcessorCheckpointer checkpointer) {
        while (inFlight.peek() != null && inFlight.peek().done) {
            lastCompleted = inFlight.poll().kinesisRecord;
        }
        if (checkpointingSuspended || lastCompleted == null || lastCompleted == lastCheckpointed) {
            return;
        }
        try {
            checkpointer.checkpoint(lastCompleted.sequenceNumber(), lastCompleted.subSequenceNumber());
            lastCheckpointed = lastCompleted;
        } catch (ShutdownException e) {
            LOGGER.debug("[{}] Processor shut down, skipping checkpoint", shardId, e);
        } catch (ThrottlingException e) {
            LOGGER.warn("[{}] Checkpoint throttled, the next one will cover it", shardId, e);
        } catch (InvalidStateException | IllegalArgumentException e) {
            LOGGER.error("[{}] Cannot checkpoint at {}", shardId, lastCompleted.sequenceNumber(), e);
        }
    }

    private boolean drain(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (inFlight.stream().anyMatch(tracked -> !tracked.done)) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(DRAIN_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private static final class Tracked {
        private final KinesisClientRecord kinesisRecord;
        private volatile boolean done;

        private Tracked(KinesisClientRecord kinesisRecord) {
            this.kinesisRecord = kinesisRecord;
        }
    }
}
