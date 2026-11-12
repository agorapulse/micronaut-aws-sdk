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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.exceptions.ThrottlingException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.kpl.ExtendedSequenceNumber;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ParallelRecordProcessorTest {

    private static final String STREAM = "test-stream";
    private static final String SHARD = "shardId-000000000001";
    private static final ParallelRecordProcessor.Timings CHECKPOINT_EVERY_BATCH = new ParallelRecordProcessor.Timings(Duration.ZERO, Duration.ofSeconds(5));
    private static final ParallelRecordProcessor.Timings CHECKPOINT_ONCE = new ParallelRecordProcessor.Timings(Duration.ofSeconds(60), Duration.ofSeconds(5));
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RecordProcessorCheckpointer checkpointer = mock(RecordProcessorCheckpointer.class);
    @SuppressWarnings("unchecked")
    private final ApplicationEventPublisher<ProcessRecordsEvent> eventPublisher = mock(ApplicationEventPublisher.class);
    private int concurrency = 12;
    private int maxAttempts = 2;
    private boolean orderedByPartitionKey = true;
    private volatile boolean workerShuttingDown;
    private final GatedConsumer consumer = new GatedConsumer();
    private ExecutorService executor;

    @BeforeEach
    void setup() {
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().uncaughtExceptionHandler((thread, error) -> { }).factory());
    }

    @AfterEach
    void tearDown() {
        consumer.releaseAll();
        executor.shutdownNow();
    }

    @Test
    void processRecords_shouldBoundParallelismAcrossTheShardsOfATask() {
        Semaphore slots = new Semaphore(3, true);
        concurrency = 3;
        ParallelRecordProcessor shard1 = processor(slots, CHECKPOINT_EVERY_BATCH);
        ParallelRecordProcessor shard2 = processor(slots, CHECKPOINT_EVERY_BATCH);
        List<KinesisClientRecord> batch1 = records("a", 5);
        List<KinesisClientRecord> batch2 = records("b", 5);
        consumer.gate(batch1);
        consumer.gate(batch2);

        CompletableFuture<Void> submissions = CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> shard1.processRecords(batch(batch1))),
                CompletableFuture.runAsync(() -> shard2.processRecords(batch(batch2))));
        await().atMost(TIMEOUT).until(() -> consumer.inFlight.get() == 3);
        assertFalse(submissions.isDone());

        consumer.releaseAll();
        submissions.join();
        awaitProcessed(10);
        assertEquals(3, consumer.maxInFlight.get());
    }

    @Test
    void processRecords_shouldNotWaitForASlowRecordBeforeTheNextBatch() {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("slow-0");

        processor.processRecords(batch(records("slow", 1)));
        processor.processRecords(batch(records("fast", 1)));
        awaitProcessed(1);

        assertEquals(List.of("fast-0"), consumer.processedSequenceNumbers());
    }

    @Test
    void processRecords_shouldProcessTheRecordsOfAPartitionKeyOneAtATimeInOrder() {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("hot-0");

        processor.processRecords(batch(sameKeyRecords("hot", 3)));
        processor.processRecords(batch(records("other", 1)));
        awaitProcessed(1);

        assertEquals(List.of("other-0"), consumer.processedSequenceNumbers());
        assertFalse(consumer.attempts.containsKey("hot-1"));

        consumer.release("hot-0");
        awaitProcessed(4);
        assertEquals(List.of("other-0", "hot-0", "hot-1", "hot-2"), consumer.processedSequenceNumbers());
    }

    @Test
    void processRecords_shouldKeepProcessingAPartitionKeyAfterAnError() throws Exception {
        consumer.errorsBySequence.add("hot-0");
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(sameKeyRecords("hot", 2)));
        awaitCheckpoint(processor, "hot-1");

        assertEquals(List.of("hot-1"), consumer.processedSequenceNumbers());
    }

    @Test
    void shutdownRequested_shouldNotStartTheRecordsStillQueuedBehindTheirPartitionKey() throws Exception {
        Semaphore slots = new Semaphore(12, true);
        ParallelRecordProcessor processor = processor(slots, new ParallelRecordProcessor.Timings(Duration.ofSeconds(60), Duration.ofMillis(100)));
        consumer.gate("hot-0");
        processor.processRecords(batch(sameKeyRecords("hot", 3)));
        await().atMost(TIMEOUT).until(() -> consumer.inFlight.get() == 1);

        processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build());
        consumer.release("hot-0");

        await().atMost(TIMEOUT).until(() -> slots.availablePermits() == 12);
        assertEquals(List.of("hot-0"), consumer.processedSequenceNumbers());
        verify(checkpointer, never()).checkpoint(anyString(), anyLong());
    }

    @Test
    void processRecords_shouldLeaveTheRecordsOfABatchWhenTheWorkerShutsDown() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_ONCE);
        processor.processRecords(batch(records("before", 1)));
        awaitProcessed(1);
        workerShuttingDown = true;

        processor.processRecords(batch(records("after", 3)));
        processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build());

        assertEquals(List.of("before-0"), consumer.processedSequenceNumbers());
        verify(checkpointer).checkpoint("before-0", 0L);
        verifyNoMoreInteractions(checkpointer);
    }

    @Test
    void processRecords_shouldCheckpointWhileABatchIsStillBeingSubmitted() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(1, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("long-1");

        CompletableFuture<Void> submission = CompletableFuture.runAsync(() -> processor.processRecords(batch(records("long", 3))));
        await().atMost(TIMEOUT).untilAsserted(() -> verify(checkpointer).checkpoint("long-0", 0L));
        assertFalse(submission.isDone());

        consumer.release("long-1");
        submission.join();
    }

    @Test
    void processRecords_shouldCountAnInterruptedHandlerAsAFailedAttemptAndKeepItsPartitionKeyGoing() throws Exception {
        consumer.interruptedSequences.add("hot-0");
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(sameKeyRecords("hot", 2)));
        awaitCheckpoint(processor, "hot-1");

        assertEquals(2, consumer.attempts.get("hot-0").get());
        assertEquals(List.of("hot-1"), consumer.processedSequenceNumbers());
        assertEquals(Set.of(), consumer.interruptedOnEntry);
    }

    @Test
    void processRecords_shouldStillSubmitTheBatchWhenTheCheckpointFails() throws Exception {
        Semaphore slots = new Semaphore(12, true);
        ParallelRecordProcessor processor = processor(slots, CHECKPOINT_EVERY_BATCH);
        doThrow(new IllegalStateException("dynamodb unavailable")).doNothing().when(checkpointer).checkpoint("first-0", 0L);
        processor.processRecords(batch(records("first", 1)));
        awaitProcessed(1);
        await().atMost(TIMEOUT).until(() -> slots.availablePermits() == 12);

        processor.processRecords(batch(records("second", 2)));
        awaitCheckpoint(processor, "second-1");

        assertEquals(List.of("first-0", "second-0", "second-1"), consumer.processedSequenceNumbers().stream().sorted().toList());
    }

    @Test
    void processRecords_shouldLeaveTheRecordsOnceTheCheckpointReportsAShutdown() throws Exception {
        Semaphore slots = new Semaphore(12, true);
        ParallelRecordProcessor processor = processor(slots, CHECKPOINT_EVERY_BATCH);
        doThrow(new ShutdownException("lease lost")).when(checkpointer).checkpoint("first-0", 0L);
        processor.processRecords(batch(records("first", 1)));
        awaitProcessed(1);
        await().atMost(TIMEOUT).until(() -> slots.availablePermits() == 12);

        processor.processRecords(batch(records("second", 2)));
        processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());

        assertEquals(List.of("first-0"), consumer.processedSequenceNumbers());
        verify(checkpointer, never()).checkpoint();
    }

    @Test
    void processRecords_shouldClearAnInterruptFlagLeftByTheHandler() throws Exception {
        consumer.interruptLeftBySequences.add("hot-0");
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(sameKeyRecords("hot", 2)));
        awaitCheckpoint(processor, "hot-1");

        assertEquals(List.of("hot-0", "hot-1"), consumer.processedSequenceNumbers());
        assertEquals(Set.of(), consumer.interruptedOnEntry);
    }

    @Test
    void shardEnded_shouldNotCheckpointTheEndWhenRecordsWereLeft() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        workerShuttingDown = true;

        processor.processRecords(batch(records("left", 2)));
        processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());

        assertTrue(consumer.processed.isEmpty());
        verifyNoMoreInteractions(checkpointer);
    }

    @Test
    void processRecords_shouldProcessTheRecordsOfAPartitionKeyInParallelWhenTheOrderIsDisabled() {
        orderedByPartitionKey = false;
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("hot-0");

        processor.processRecords(batch(sameKeyRecords("hot", 3)));
        awaitProcessed(2);

        assertEquals(List.of("hot-1", "hot-2"), consumer.processedSequenceNumbers().stream().sorted().toList());
    }

    @Test
    void processRecords_shouldHoldASingleSlotForABusyPartitionKey() {
        ParallelRecordProcessor processor = processor(new Semaphore(2, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("hot-0");

        processor.processRecords(batch(sameKeyRecords("hot", 5)));
        processor.processRecords(batch(records("other", 3)));
        awaitProcessed(3);

        assertEquals(List.of("other-0", "other-1", "other-2"), consumer.processedSequenceNumbers().stream().sorted().toList());
    }

    @Test
    void processRecords_shouldKeepTheOrderOfAPartitionKeyAcrossRetriesAndDrops() {
        consumer.failuresBySequence.put("hot-0", new AtomicInteger(1));
        consumer.failuresBySequence.put("hot-1", new AtomicInteger(Integer.MAX_VALUE));
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(sameKeyRecords("hot", 3)));
        awaitProcessed(2);

        assertEquals(List.of("hot-0", "hot-2"), consumer.processedSequenceNumbers());
        assertEquals(2, consumer.attempts.get("hot-0").get());
        assertEquals(2, consumer.attempts.get("hot-1").get());
    }

    @Test
    void processRecords_shouldNeverCheckpointPastARecordStillInFlight() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        consumer.gate("key-0");

        processor.processRecords(batch(records("key", 3)));
        awaitProcessed(2);
        processor.processRecords(batch(List.of()));
        verify(checkpointer, never()).checkpoint(anyString(), anyLong());

        consumer.release("key-0");
        awaitCheckpoint(processor, "key-2");
        verify(checkpointer).checkpoint("key-2", 0L);
    }

    @Test
    void processRecords_shouldLetTheCheckpointPassADroppedRecord() throws Exception {
        maxAttempts = 2;
        consumer.failuresBySequence.put("key-0", new AtomicInteger(Integer.MAX_VALUE));
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(records("key", 2)));
        awaitCheckpoint(processor, "key-1");

        assertEquals(List.of("key-1"), consumer.processedSequenceNumbers());
        assertEquals(2, consumer.attempts.get("key-0").get());
        verify(checkpointer).checkpoint("key-1", 0L);
    }

    @Test
    void processRecords_shouldDropARecordThrowingAnErrorAndStillCheckpoint() throws Exception {
        consumer.errorsBySequence.add("key-0");
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(records("key", 2)));
        awaitCheckpoint(processor, "key-1");

        assertEquals(List.of("key-1"), consumer.processedSequenceNumbers());
        verify(checkpointer).checkpoint("key-1", 0L);
    }

    @Test
    void processRecords_shouldRetryAFailedRecordThenSucceed() {
        consumer.failuresBySequence.put("key-0", new AtomicInteger(1));
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);

        processor.processRecords(batch(records("key", 1)));
        awaitProcessed(1);

        assertEquals(2, consumer.attempts.get("key-0").get());
    }

    @Test
    void processRecords_shouldCheckpointAtMostOncePerInterval() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_ONCE);

        processor.processRecords(batch(records("key", 1)));
        awaitProcessed(1);
        processor.processRecords(batch(records("key", 1)));
        awaitProcessed(2);
        processor.processRecords(batch(List.of()));

        verify(checkpointer, atMost(1)).checkpoint(anyString(), anyLong());
    }

    @Test
    void shutdownRequested_shouldWaitForInFlightRecordsThenCheckpoint() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_ONCE);
        consumer.gate("key-0");
        processor.processRecords(batch(records("key", 1)));

        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(() ->
                processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build()));
        await().atMost(TIMEOUT).until(() -> consumer.inFlight.get() == 1);
        assertFalse(shutdown.isDone());

        consumer.release("key-0");
        shutdown.join();
        assertEquals(1, consumer.processed.size());
        verify(checkpointer).checkpoint("key-0", 0L);
    }

    @Test
    void shutdownRequested_shouldGiveUpWaitingAfterTheTimeoutWithoutCheckpointing() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), new ParallelRecordProcessor.Timings(Duration.ofSeconds(60), Duration.ofMillis(100)));
        consumer.gate("key-0");
        processor.processRecords(batch(records("key", 1)));

        processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build());

        assertTrue(consumer.processed.isEmpty());
        verify(checkpointer, never()).checkpoint(anyString(), anyLong());
    }

    @Test
    void shardEnded_shouldWaitThenCheckpointTheEnd_leaseLost_shouldNot() throws Exception {
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_ONCE);
        consumer.gate("key-0");
        processor.processRecords(batch(records("key", 1)));
        await().atMost(TIMEOUT).until(() -> consumer.inFlight.get() == 1);

        processor.leaseLost(LeaseLostInput.builder().build());
        CompletableFuture<Void> shardEnd = CompletableFuture.runAsync(() ->
                processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build()));
        assertFalse(shardEnd.isDone());

        consumer.release("key-0");
        shardEnd.join();
        assertEquals(1, consumer.processed.size());
        verify(checkpointer).checkpoint();
        verifyNoMoreInteractions(checkpointer);
    }

    @Test
    void processRecords_shouldLeaveTheRecordsThatCannotBeSubmitted() {
        Semaphore slots = new Semaphore(12, true);
        ParallelRecordProcessor processor = processor(slots, CHECKPOINT_EVERY_BATCH);
        processor.processRecords(batch(records("key", 1)));
        awaitCheckpoint(processor, "key-0");
        await().atMost(TIMEOUT).until(() -> slots.availablePermits() == 12);
        executor.shutdown();
        clearInvocations(checkpointer);

        processor.processRecords(batch(records("rejected", 2)));
        processor.shutdownRequested(ShutdownRequestedInput.builder().checkpointer(checkpointer).build());
        processor.shardEnded(ShardEndedInput.builder().checkpointer(checkpointer).build());

        assertEquals(12, slots.availablePermits());
        verifyNoMoreInteractions(checkpointer);
    }

    @Test
    void processRecords_shouldRetryAThrottledCheckpointAtTheNextOpportunity() throws Exception {
        doThrow(new ThrottlingException("throttled")).doNothing().when(checkpointer).checkpoint("key-0", 0L);
        ParallelRecordProcessor processor = processor(new Semaphore(12, true), CHECKPOINT_EVERY_BATCH);
        processor.processRecords(batch(records("key", 1)));

        await().atMost(TIMEOUT).untilAsserted(() -> {
            processor.processRecords(batch(List.of()));
            verify(checkpointer, times(2)).checkpoint("key-0", 0L);
        });
        processor.processRecords(batch(List.of()));
        verify(checkpointer, times(2)).checkpoint("key-0", 0L);
    }

    @Test
    void processRecords_shouldNotPublishAnEventForAnEmptyBatch() {
        processor(new Semaphore(12, true), CHECKPOINT_ONCE).processRecords(batch(List.of()));

        verify(eventPublisher, never()).publishEventAsync(any());
    }

    @Test
    void processRecords_shouldPublishTheBatchEvent() {
        processor(new Semaphore(12, true), CHECKPOINT_ONCE).processRecords(ProcessRecordsInput.builder()
                .records(records("key", 4))
                .checkpointer(checkpointer)
                .millisBehindLatest(1500L)
                .build());

        ArgumentCaptor<ProcessRecordsEvent> event = ArgumentCaptor.forClass(ProcessRecordsEvent.class);
        verify(eventPublisher).publishEventAsync(event.capture());
        assertEquals(STREAM, event.getValue().getStream());
        assertEquals(SHARD, event.getValue().getShardId());
        assertEquals(1500L, event.getValue().getMillisBehindLatest());
        assertEquals(4, event.getValue().getRecordCount());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private ParallelRecordProcessor processor(Semaphore slots, ParallelRecordProcessor.Timings timings) {
        ParallelRecordProcessor processor = new ParallelRecordProcessor(consumer, executor, slots, new ParallelRecordProcessor.Settings(concurrency, maxAttempts, orderedByPartitionKey), eventPublisher, STREAM, timings, () -> workerShuttingDown);
        processor.initialize(InitializationInput.builder().shardId(SHARD).extendedSequenceNumber(ExtendedSequenceNumber.TRIM_HORIZON).build());
        return processor;
    }

    private ProcessRecordsInput batch(List<KinesisClientRecord> kinesisRecords) {
        return ProcessRecordsInput.builder().records(kinesisRecords).checkpointer(checkpointer).millisBehindLatest(0L).build();
    }

    private void awaitProcessed(int count) {
        await().atMost(TIMEOUT).until(() -> consumer.processed.size() >= count);
    }

    // Empty batches stand in for the KCL polling the shard until the in-flight records let the checkpoint advance.
    private void awaitCheckpoint(ParallelRecordProcessor processor, String sequenceNumber) {
        await().atMost(TIMEOUT).until(() -> {
            processor.processRecords(batch(List.of()));
            return mockingDetails(checkpointer).getInvocations().stream()
                    .anyMatch(invocation -> "checkpoint".equals(invocation.getMethod().getName())
                            && invocation.getArguments().length == 2
                            && sequenceNumber.equals(invocation.getArguments()[0]));
        });
    }

    private static List<KinesisClientRecord> records(String prefix, int count) {
        return records(prefix, count, false);
    }

    private static List<KinesisClientRecord> sameKeyRecords(String partitionKey, int count) {
        return records(partitionKey, count, true);
    }

    private static List<KinesisClientRecord> records(String prefix, int count, boolean sameKey) {
        return IntStream.range(0, count)
                .mapToObj(i -> KinesisClientRecord.builder()
                        .partitionKey(sameKey ? prefix : prefix + "-" + i)
                        .sequenceNumber(prefix + "-" + i)
                        .data(ByteBuffer.wrap(("{\"i\":" + i + "}").getBytes(StandardCharsets.UTF_8)))
                        .build())
                .toList();
    }

    // Records are gated by sequence number: a gated record blocks until the test releases it, the others finish at once.
    private static class GatedConsumer implements BiConsumer<String, KinesisClientRecord> {
        final List<KinesisClientRecord> processed = new CopyOnWriteArrayList<>();
        final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        final Map<String, AtomicInteger> failuresBySequence = new ConcurrentHashMap<>();
        final Set<String> errorsBySequence = ConcurrentHashMap.newKeySet();
        final Set<String> interruptedSequences = ConcurrentHashMap.newKeySet();
        final Set<String> interruptedOnEntry = ConcurrentHashMap.newKeySet();
        final Set<String> interruptLeftBySequences = ConcurrentHashMap.newKeySet();
        final Map<String, CountDownLatch> gates = new ConcurrentHashMap<>();
        final AtomicInteger inFlight = new AtomicInteger();
        final AtomicInteger maxInFlight = new AtomicInteger();

        List<String> processedSequenceNumbers() {
            return processed.stream().map(KinesisClientRecord::sequenceNumber).toList();
        }

        void gate(String sequenceNumber) {
            gates.put(sequenceNumber, new CountDownLatch(1));
        }

        void gate(List<KinesisClientRecord> kinesisRecords) {
            kinesisRecords.forEach(kinesisRecord -> gate(kinesisRecord.sequenceNumber()));
        }

        void release(String sequenceNumber) {
            gates.get(sequenceNumber).countDown();
        }

        void releaseAll() {
            gates.values().forEach(CountDownLatch::countDown);
        }

        @Override
        public void accept(String data, KinesisClientRecord kinesisRecord) {
            attempts.computeIfAbsent(kinesisRecord.sequenceNumber(), k -> new AtomicInteger()).incrementAndGet();
            if (Thread.currentThread().isInterrupted()) {
                interruptedOnEntry.add(kinesisRecord.sequenceNumber());
            }
            if (interruptedSequences.contains(kinesisRecord.sequenceNumber())) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted " + kinesisRecord.sequenceNumber());
            }
            if (errorsBySequence.contains(kinesisRecord.sequenceNumber())) {
                throw new NoClassDefFoundError("failing " + kinesisRecord.sequenceNumber());
            }
            AtomicInteger failures = failuresBySequence.get(kinesisRecord.sequenceNumber());
            if (failures != null && failures.getAndDecrement() > 0) {
                throw new IllegalStateException("failing " + kinesisRecord.sequenceNumber());
            }
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                CountDownLatch gate = gates.get(kinesisRecord.sequenceNumber());
                if (gate != null) {
                    gate.await();
                }
                processed.add(kinesisRecord);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
            }
            if (interruptLeftBySequences.contains(kinesisRecord.sequenceNumber())) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
