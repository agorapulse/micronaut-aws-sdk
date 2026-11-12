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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Feeds several shards sharing the slots with many records of a few partition keys, with random failures, and checks
 * the bounds, the order of each partition key and the final checkpoint.
 */
class ParallelRecordProcessorStressTest {

    private static final int SHARDS = 3;
    private static final int SLOTS = 8;
    private static final int KEYS_PER_SHARD = 12;
    private static final int RECORDS_PER_SHARD = 1000;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    @SuppressWarnings("unchecked")
    private final ApplicationEventPublisher<ProcessRecordsEvent> eventPublisher = mock(ApplicationEventPublisher.class);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void ordered_shouldProcessEveryRecordOnceInTheOrderOfItsPartitionKeyWithinTheSlots() {
        Recorder recorder = run(true);

        assertEquals(SHARDS * RECORDS_PER_SHARD, recorder.succeeded.get());
        assertTrue(recorder.maxInFlight.get() <= SLOTS, "max in flight " + recorder.maxInFlight.get());
        assertEquals(1, recorder.maxInFlightPerKey.get());
        assertEquals(List.of(), List.copyOf(recorder.orderViolations));
    }

    @Test
    void unordered_shouldProcessEveryRecordOnceWithinTheSlots() {
        Recorder recorder = run(false);

        assertEquals(SHARDS * RECORDS_PER_SHARD, recorder.succeeded.get());
        assertTrue(recorder.maxInFlight.get() <= SLOTS, "max in flight " + recorder.maxInFlight.get());
    }

    private Recorder run(boolean ordered) {
        Recorder recorder = new Recorder();
        Semaphore slots = new Semaphore(SLOTS, true);
        ParallelRecordProcessor.Settings settings = new ParallelRecordProcessor.Settings(SLOTS, 3, ordered);
        ParallelRecordProcessor.Timings timings = new ParallelRecordProcessor.Timings(Duration.ZERO, Duration.ofSeconds(5));
        List<CompletableFuture<Void>> shards = new ArrayList<>();
        for (int shard = 0; shard < SHARDS; shard++) {
            int shardIndex = shard;
            shards.add(CompletableFuture.runAsync(() -> feedShard(shardIndex, slots, settings, timings, recorder)));
        }
        CompletableFuture.allOf(shards.toArray(CompletableFuture[]::new)).join();
        assertEquals(SLOTS, slots.availablePermits());
        return recorder;
    }

    private void feedShard(int shard, Semaphore slots, ParallelRecordProcessor.Settings settings, ParallelRecordProcessor.Timings timings, Recorder recorder) {
        RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
        ParallelRecordProcessor processor = new ParallelRecordProcessor(recorder, executor, slots, settings, eventPublisher, "stream", timings, () -> false);
        processor.initialize(InitializationInput.builder().shardId("shard-" + shard).extendedSequenceNumber(ExtendedSequenceNumber.TRIM_HORIZON).build());
        Random random = new Random(shard);
        int next = 0;
        while (next < RECORDS_PER_SHARD) {
            int size = Math.min(RECORDS_PER_SHARD - next, 1 + random.nextInt(40));
            List<KinesisClientRecord> batch = new ArrayList<>();
            for (int i = 0; i < size; i++, next++) {
                // a skewed key choice: a third of the records go to the first key of the shard
                int key = random.nextInt(3) == 0 ? 0 : random.nextInt(KEYS_PER_SHARD);
                batch.add(KinesisClientRecord.builder()
                    .partitionKey("s" + shard + "-k" + key)
                    .sequenceNumber(String.format("%d-%06d", shard, next))
                    .data(ByteBuffer.wrap("{}".getBytes(StandardCharsets.UTF_8)))
                    .build());
            }
            processor.processRecords(ProcessRecordsInput.builder().records(batch).checkpointer(checkpointer).millisBehindLatest(0L).build());
        }
        String last = String.format("%d-%06d", shard, RECORDS_PER_SHARD - 1);
        await().atMost(TIMEOUT).until(() -> {
            processor.processRecords(ProcessRecordsInput.builder().records(List.of()).checkpointer(checkpointer).millisBehindLatest(0L).build());
            return mockingDetails(checkpointer).getInvocations().stream()
                .anyMatch(invocation -> invocation.getArguments().length == 2 && last.equals(invocation.getArguments()[0]));
        });
    }

    private static class Recorder implements BiConsumer<String, KinesisClientRecord> {
        final AtomicInteger succeeded = new AtomicInteger();
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger maxInFlight = new AtomicInteger();
        final AtomicInteger maxInFlightPerKey = new AtomicInteger();
        final Map<String, AtomicInteger> inFlightByKey = new ConcurrentHashMap<>();
        final Map<String, String> lastSequenceByKey = new ConcurrentHashMap<>();
        final Set<String> failedOnce = ConcurrentHashMap.newKeySet();
        final Queue<String> orderViolations = new ConcurrentLinkedQueue<>();

        @Override
        public void accept(String data, KinesisClientRecord kinesisRecord) {
            String key = kinesisRecord.partitionKey();
            String sequence = kinesisRecord.sequenceNumber();
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            AtomicInteger keyInFlight = inFlightByKey.computeIfAbsent(key, k -> new AtomicInteger());
            maxInFlightPerKey.accumulateAndGet(keyInFlight.incrementAndGet(), Math::max);
            try {
                LockSupport.parkNanos(Math.floorMod(sequence.hashCode(), 100) * 1000L);
                // one record in ten fails its first attempt and is retried
                if (Math.floorMod(sequence.hashCode(), 10) == 0 && failedOnce.add(sequence)) {
                    throw new IllegalStateException("first attempt of " + sequence);
                }
                String previous = lastSequenceByKey.put(key, sequence);
                if (previous != null && previous.compareTo(sequence) >= 0) {
                    orderViolations.add(key + ": " + sequence + " after " + previous);
                }
                succeeded.incrementAndGet();
            } finally {
                keyInFlight.decrementAndGet();
                inFlight.decrementAndGet();
            }
        }
    }
}
