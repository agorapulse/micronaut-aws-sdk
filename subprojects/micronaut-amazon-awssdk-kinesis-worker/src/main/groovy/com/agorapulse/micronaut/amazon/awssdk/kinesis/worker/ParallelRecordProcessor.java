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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

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
    record Settings(int concurrency, int maxAttempts, boolean orderedByPartitionKey) {
        Settings {
            maxAttempts = Math.max(1, maxAttempts);
        }
    }

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
    private final BooleanSupplier workerShuttingDown;

    private final ConcurrentLinkedQueue<Tracked> inFlight = new ConcurrentLinkedQueue<>();
    private final Map<String, Tracked> lastByPartitionKey = new ConcurrentHashMap<>();
    private String shardId = "";
    private long nextCheckpointTimeInMillis;
    private KinesisClientRecord lastCompleted;
    private KinesisClientRecord lastCheckpointed;
    private volatile boolean checkpointingSuspended;
    private volatile boolean stopped;

    ParallelRecordProcessor(
        BiConsumer<String, KinesisClientRecord> consumer,
        ExecutorService executor,
        Semaphore slots,
        Settings settings,
        ApplicationEventPublisher<ProcessRecordsEvent> eventPublisher,
        String stream,
        Timings timings,
        BooleanSupplier workerShuttingDown
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
        this.workerShuttingDown = workerShuttingDown;
    }

    @Override
    public void initialize(InitializationInput input) {
        shardId = input.shardId();
        LOGGER.info("[{}] Initialized at sequence {}", shardId, input.extendedSequenceNumber());
    }

    // Blocks the shard thread while every slot is busy or the shard has as many unfinished records as the concurrency,
    // so the KCL stops reading this shard until a record finishes. The records left when the worker shuts down are not
    // started: no checkpoint passes them, so the next owner of the lease processes them.
    @Override
    public void processRecords(ProcessRecordsInput input) {
        for (KinesisClientRecord kinesisRecord : input.records()) {
            if (workerShuttingDown.getAsBoolean()) {
                LOGGER.info("[{}] Worker shutting down, leaving the records from {} for the next owner of the lease", shardId, kinesisRecord.sequenceNumber());
                return;
            }
            checkpointIfDue(input.checkpointer());
            if (!submit(kinesisRecord)) {
                suspendCheckpointing("record " + kinesisRecord.sequenceNumber() + " could not be submitted");
                return;
            }
        }
        if (!input.records().isEmpty()) {
            eventPublisher.publishEventAsync(new ProcessRecordsEvent(stream, shardId, input.millisBehindLatest(), input.records().size()));
        }
        checkpointIfDue(input.checkpointer());
    }

    private void checkpointIfDue(RecordProcessorCheckpointer checkpointer) {
        if (System.currentTimeMillis() >= nextCheckpointTimeInMillis) {
            checkpointCompleted(checkpointer);
            nextCheckpointTimeInMillis = System.currentTimeMillis() + timings.checkpointInterval().toMillis();
        }
    }

    @Override
    public void leaseLost(LeaseLostInput input) {
        stopped = true;
        LOGGER.info("[{}] Lease lost with {} records in flight", shardId, inFlight.size());
    }

    // Without the shard-end checkpoint the KCL fails the shutdown and calls this again later.
    @Override
    public void shardEnded(ShardEndedInput input) {
        if (checkpointingSuspended || !drain(SHARD_END_DRAIN_TIMEOUT)) {
            LOGGER.warn("[{}] Shard ended with {} records still in flight or checkpointing suspended, leaving the shard end for later", shardId, inFlight.size());
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
        stopped = true;
        checkpointCompleted(input.checkpointer());
    }

    // The shard thread takes the slot itself, so the shards of a worker share the slots fairly.
    private boolean submit(KinesisClientRecord kinesisRecord) {
        try {
            pending.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        Tracked tracked = new Tracked(kinesisRecord);
        if (!checkpointingSuspended) {
            inFlight.add(tracked);
        }
        if (orderedByPartitionKey && queuedBehindItsPartitionKey(tracked)) {
            return true;
        }
        try {
            slots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abandon(tracked);
            return false;
        }
        try {
            executor.execute(() -> run(tracked));
        } catch (RejectedExecutionException e) {
            slots.release();
            abandon(tracked);
            return false;
        }
        return true;
    }

    // The link is set under the map's lock, so the thread running the partition key always sees the record queued behind.
    private boolean queuedBehindItsPartitionKey(Tracked tracked) {
        Tracked[] previous = new Tracked[1];
        lastByPartitionKey.compute(tracked.partitionKey(), (key, last) -> {
            previous[0] = last;
            if (last != null) {
                last.next = tracked;
            }
            return tracked;
        });
        return previous[0] != null;
    }

    private void abandon(Tracked tracked) {
        inFlight.remove(tracked);
        lastByPartitionKey.remove(tracked.partitionKey(), tracked);
        pending.release();
    }

    // Runs the record, then the records of its partition key queued behind it, on the same slot: a busy key holds one slot.
    private void run(Tracked first) {
        try {
            boolean interrupted = false;
            for (Tracked current = first; current != null; current = next(current)) {
                if (!interrupted && !stopped) {
                    interrupted = !process(current);
                }
                pending.release();
            }
        } finally {
            slots.release();
        }
    }

    // An Error escaping the handler still marks the record done, otherwise it would hold the checkpoint and its key forever.
    private boolean process(Tracked tracked) {
        try {
            tracked.done = processWithRetries(tracked.kinesisRecord);
        } catch (Throwable error) {
            LOGGER.error("[{}] Dropping record {} after an unexpected error", shardId, tracked.kinesisRecord.sequenceNumber(), error);
            tracked.done = true;
        }
        return tracked.done;
    }

    private Tracked next(Tracked current) {
        if (!orderedByPartitionKey) {
            return null;
        }
        Tracked last = lastByPartitionKey.compute(current.partitionKey(), (key, tail) -> tail == current ? null : tail);
        return last == null ? null : current.next;
    }

    // The KCL skips the rest of a batch whose records could not be submitted, so no later checkpoint may pass them.
    // The records keep being processed, untracked, until the lease is taken over.
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
                if (Thread.currentThread().isInterrupted()) {
                    suspendCheckpointing("record " + kinesisRecord.sequenceNumber() + " was interrupted while being processed");
                    return false;
                }
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

    // Only the shard thread adds and polls; a processing thread flags its record as done, or clears the queue when it
    // suspends checkpointing, which the check below covers.
    private void checkpointCompleted(RecordProcessorCheckpointer checkpointer) {
        for (Tracked head = inFlight.peek(); head != null && head.done; head = inFlight.peek()) {
            inFlight.poll();
            lastCompleted = head.kinesisRecord;
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
        private volatile Tracked next;

        private Tracked(KinesisClientRecord kinesisRecord) {
            this.kinesisRecord = kinesisRecord;
        }

        private String partitionKey() {
            return Objects.requireNonNullElse(kinesisRecord.partitionKey(), "");
        }
    }
}
