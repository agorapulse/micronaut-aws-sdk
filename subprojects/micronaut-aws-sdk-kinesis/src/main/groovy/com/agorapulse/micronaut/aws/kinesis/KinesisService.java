/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.services.kinesis.model.CreateStreamResult;
import com.amazonaws.services.kinesis.model.DeleteStreamResult;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.MergeShardsResult;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.kinesis.model.ShardIteratorType;
import com.amazonaws.services.kinesis.model.SplitShardResult;
import com.amazonaws.services.kinesis.model.StreamDescription;
import com.amazonaws.services.kinesis.model.StreamStatus;
import io.micronaut.core.async.annotation.SingleResult;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * KinesisService provides middle-level API access to the Kinesis streams.
 */
public interface KinesisService {

    /**
     * @return the name of the default stream
     */
    String getDefaultStreamName();

    /**
     * Creates stream of given name and shard count.
     *
     * @param streamName the name of the stream
     * @param shardCount number of the shards to be created
     * @return create stream result
     */
    CreateStreamResult createStream(String streamName, int shardCount);

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and one shard.
     *
     * @param streamName the name of the stream
     * @return create stream result
     */
    default CreateStreamResult createStream(String streamName) {
        return createStream(streamName, 1);
    }

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and given shard count.
     * @param shardCount number of the shards to be created
     * @return create stream result
     */
    default CreateStreamResult createStream(int shardCount) {
        return createStream(getDefaultStreamName(), shardCount);
    }

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and one shard.
     * @return create stream result
     */
    default CreateStreamResult createStream() {
        return createStream(1);
    }

    /**
     * Decodes the body of the record into its string representation.
     * @param record record to be processed
     * @return string representation of the body of the record
     */
    String decodeRecordData(Record record);

    /**
     * Deletes stream of given name.
     * @param streamName the name of the stream
     * @return delete stream result
     */
    DeleteStreamResult deleteStream(String streamName);

    /**
     * Deletes stream of {@link #getDefaultStreamName()} name.
     *
     * @return delete stream result
     */
    default DeleteStreamResult deleteStream() {
        return deleteStream(getDefaultStreamName());
    }

    /**
     * Describes the stream of given name.
     * @param streamName the name of the stream
     * @return describe stream result
     */
    DescribeStreamResult describeStream(String streamName);

    /**
     * Describe stream of {@link #getDefaultStreamName()} name.
     * @return describe stream result
     */
    default DescribeStreamResult describeStream() {
        return describeStream(getDefaultStreamName());
    }

    /**
     * Returns shard of given stream by its id.
     * @param streamName the name of the stream
     * @param shardId the id of the shard
     * @return shard of given stream by its id
     */
    Shard getShard(String streamName, String shardId);

    /**
     * Returns shard of default stream by its id.
     * @param shardId the id of the shard
     * @return shard of default stream by its id
     */
    default Shard getShard(String shardId) {
        return getShard(getDefaultStreamName(), shardId);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @return
     */
    @SingleResult
    default Publisher<Record> getShardOldestRecord(String streamName, Shard shard) {
        return Flux.from(getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", 1)).take(1).singleOrEmpty();
    }

    /**
     *
     * @param shard the shard to be used
     * @return
     */
    @SingleResult
    default Publisher<Record> getShardOldestRecord(Shard shard) {
        return getShardOldestRecord(getDefaultStreamName(), shard);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param sequenceNumber
     * @return
     */
    @SingleResult
    default Publisher<Record> getShardRecordAtSequenceNumber(String streamName, Shard shard, String sequenceNumber) {
        return Flux.from(getShardRecords(streamName, shard, ShardIteratorType.AT_SEQUENCE_NUMBER, sequenceNumber, 1)).take(1).singleOrEmpty();
    }

    /**
     *
     * @param shard the shard to be used
     * @param sequenceNumber
     * @return
     */
    @SingleResult
    default Publisher<Record> getShardRecordAtSequenceNumber(Shard shard, String sequenceNumber) {
        return getShardRecordAtSequenceNumber(getDefaultStreamName(), shard, sequenceNumber);
    }

    /**
     *
     * @return
     */
    List<String> listStreamNames();

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @param batchLimit
     * @return
     */
    Publisher<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit);

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @return
     */
    default Publisher<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber) {
        return getShardRecords(streamName, shard, shardIteratorType, startingSequenceNumber, 0);
    }

    /**
     *
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @param batchLimit
     * @return
     */
    default Publisher<Record> getShardRecords(Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit) {
        return getShardRecords(getDefaultStreamName(), shard, shardIteratorType, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @return
     */
    default Publisher<Record> getShardRecords(Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber) {
        return getShardRecords(getDefaultStreamName(), shard, shardIteratorType, startingSequenceNumber, 0);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @param batchLimit
     * @return
     */
    default Publisher<Record> listShardRecordsAfterSequenceNumber(String streamName, Shard shard, String startingSequenceNumber, int batchLimit) {
        return getShardRecords(streamName, shard, ShardIteratorType.AFTER_SEQUENCE_NUMBER, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @return
     */
    default Publisher<Record> listShardRecordsAfterSequenceNumber(String streamName, Shard shard, String startingSequenceNumber) {
        return getShardRecords(streamName, shard, ShardIteratorType.AFTER_SEQUENCE_NUMBER, startingSequenceNumber, 0);
    }

    /**
     *
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @param batchLimit
     * @return
     */
    default Publisher<Record> listShardRecordsAfterSequenceNumber(Shard shard, String startingSequenceNumber, int batchLimit) {
        return listShardRecordsAfterSequenceNumber(getDefaultStreamName(), shard, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @return
     */
    default Publisher<Record> listShardRecordsAfterSequenceNumber(Shard shard, String startingSequenceNumber) {
        return listShardRecordsAfterSequenceNumber(getDefaultStreamName(), shard, startingSequenceNumber, 0);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param batchLimit
     * @return
     */
    default Publisher<Record> listShardRecordsFromHorizon(String streamName, Shard shard, int batchLimit) {
        return getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", batchLimit);
    }
    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @return
     */
    default Publisher<Record> listShardRecordsFromHorizon(String streamName, Shard shard) {
        return getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", 0);
    }

    /**
     *
     * @param shard the shard to be used
     * @param batchLimit
     * @return
     */
    default Publisher<Record> listShardRecordsFromHorizon(Shard shard, int batchLimit) {
        return listShardRecordsFromHorizon(getDefaultStreamName(), shard, batchLimit);
    }
    /**
     *
     * @param shard the shard to be used
     * @return
     */
    default Publisher<Record> listShardRecordsFromHorizon(Shard shard) {
        return listShardRecordsFromHorizon(getDefaultStreamName(), shard, 0);
    }

    /**
     *
     * @param streamName the name of the stream
     * @return
     */
    default List<Shard> listShards(String streamName) {
        return Optional
            .ofNullable(describeStream(streamName))
            .map(DescribeStreamResult::getStreamDescription)
            .map(StreamDescription::getShards)
            .orElse(Collections.emptyList());
    }

    /**
     *
     * @return
     */
    default List<Shard> listShards() {
        return listShards(getDefaultStreamName());
    }

    /**
     *
     * @param streamName the name of the stream
     * @param event
     * @param sequenceNumberForOrdering
     * @return
     */
    PutRecordResult putEvent(String streamName, Event event, String sequenceNumberForOrdering);

    /**
     *
     * @param streamName the name of the stream
     * @param event
     * @return
     */
    default PutRecordResult putEvent(String streamName, Event event) {
        return putEvent(streamName, event, "");
    }

    /**
     *
     * @param event
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResult putEvent(Event event, String sequenceNumberForOrdering) {
        return putEvent(getDefaultStreamName(), event, sequenceNumberForOrdering);
    }

    /**
     *
     * @param event
     * @return
     */
    default PutRecordResult putEvent(Event event) {
        return putEvent(getDefaultStreamName(), event, "");
    }

    /**
     *
     * @param streamName the name of the stream
     * @param events
     * @return
     */
    PutRecordsResult putEvents(String streamName, List<Event> events);

    /**
     *
     * @param events
     * @return
     */
    default PutRecordsResult putEvents(List<Event> events) {
        return putEvents(getDefaultStreamName(), events);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResult putRecord(String streamName, String partitionKey, String data, String sequenceNumberForOrdering) {
        return putRecord(streamName, partitionKey, data.getBytes(), sequenceNumberForOrdering);
    }

    /**
     *
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResult putRecord(String partitionKey, String data, String sequenceNumberForOrdering) {
        return putRecord(getDefaultStreamName(), partitionKey, data, sequenceNumberForOrdering);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    PutRecordResult putRecord(String streamName, String partitionKey, byte[] data, String sequenceNumberForOrdering);

    /**
     *
     * @param streamName the name of the stream
     * @param partitionKey
     * @param data
     * @return
     */
    default PutRecordResult putRecord(String streamName, String partitionKey, byte[] data) {
        return putRecord(streamName, partitionKey, data, null);
    }

    /**
     *
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResult putRecord(String partitionKey, byte[] data, String sequenceNumberForOrdering) {
        return putRecord(getDefaultStreamName(), partitionKey, data, sequenceNumberForOrdering);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param records
     * @return
     */
    PutRecordsResult putRecords(String streamName, List<PutRecordsRequestEntry> records);

    /**
     *
     * @param records
     * @return
     */
    default PutRecordsResult putRecords(List<PutRecordsRequestEntry> records) {
        return putRecords(getDefaultStreamName(), records);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shardId1
     * @param shardId2
     */
    MergeShardsResult mergeShards(String streamName, String shardId1, String shardId2);

    /**
     *
     * @param shardId1
     * @param shardId2
     */
    default MergeShardsResult mergeShards(String shardId1, String shardId2) {
        return mergeShards(getDefaultStreamName(), shardId1, shardId2);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param newStartingHashKey
     */
    SplitShardResult splitShard(String streamName, Shard shard, String newStartingHashKey);

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     */
    default SplitShardResult splitShard(String streamName, Shard shard) {
        return splitShard(streamName, shard, "");
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shardId the id of the shard
     */
    default SplitShardResult splitShard(String streamName, String shardId) {
        Shard shard = getShard(shardId);
        if (shard == null) {
            throw new IllegalArgumentException("Shard not found for id " + shardId);
        }
        return splitShard(streamName, shard);
    }

    /**
     *
     * @param shardId the id of the shard
     */
    default SplitShardResult splitShard(String shardId) {
        return splitShard(getDefaultStreamName(), shardId);
    }

    default void waitForActive() {
        waitForStatus(StreamStatus.ACTIVE);
    }


    default void waitForStatus(StreamStatus status) {
        waitForStatus(getDefaultStreamName(), status);
    }

    default void waitForActive(String streamName) {
        waitForStatus(streamName, StreamStatus.ACTIVE);
    }

    void waitForStatus(String streamName, StreamStatus status);
}
