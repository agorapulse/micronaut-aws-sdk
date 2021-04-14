/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import software.amazon.awssdk.services.kinesis.model.*;

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
    CreateStreamResponse createStream(String streamName, int shardCount);

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and one shard.
     *
     * @param streamName the name of the stream
     * @return create stream result
     */
    default CreateStreamResponse createStream(String streamName) {
        return createStream(streamName, 1);
    }

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and given shard count.
     * @param shardCount number of the shards to be created
     * @return create stream result
     */
    default CreateStreamResponse createStream(int shardCount) {
        return createStream(getDefaultStreamName(), shardCount);
    }

    /**
     * Creates stream of {@link #getDefaultStreamName()} name and one shard.
     * @return create stream result
     */
    default CreateStreamResponse createStream() {
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
    DeleteStreamResponse deleteStream(String streamName);

    /**
     * Deletes stream of {@link #getDefaultStreamName()} name.
     *
     * @return delete stream result
     */
    default DeleteStreamResponse deleteStream() {
        return deleteStream(getDefaultStreamName());
    }

    /**
     * Describes the stream of given name.
     * @param streamName the name of the stream
     * @return describe stream result
     */
    DescribeStreamResponse describeStream(String streamName);

    /**
     * Describe stream of {@link #getDefaultStreamName()} name.
     * @return describe stream result
     */
    default DescribeStreamResponse describeStream() {
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
    default Maybe<Record> getShardOldestRecord(String streamName, Shard shard) {
        return getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", 1).take(1).singleElement();
    }

    /**
     *
     * @param shard the shard to be used
     * @return
     */
    default Maybe<Record> getShardOldestRecord(Shard shard) {
        return getShardOldestRecord(getDefaultStreamName(), shard);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param sequenceNumber
     * @return
     */
    default Maybe<Record> getShardRecordAtSequenceNumber(String streamName, Shard shard, String sequenceNumber) {
        return getShardRecords(streamName, shard, ShardIteratorType.AT_SEQUENCE_NUMBER, sequenceNumber, 1).take(1).singleElement();
    }

    /**
     *
     * @param shard the shard to be used
     * @param sequenceNumber
     * @return
     */
    default Maybe<Record> getShardRecordAtSequenceNumber(Shard shard, String sequenceNumber) {
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
    Flowable<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit);

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @return
     */
    default Flowable<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber) {
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
    default Flowable<Record> getShardRecords(Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit) {
        return getShardRecords(getDefaultStreamName(), shard, shardIteratorType, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param shard the shard to be used
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @return
     */
    default Flowable<Record> getShardRecords(Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber) {
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
    default Flowable<Record> listShardRecordsAfterSequenceNumber(String streamName, Shard shard, String startingSequenceNumber, int batchLimit) {
        return getShardRecords(streamName, shard, ShardIteratorType.AFTER_SEQUENCE_NUMBER, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @return
     */
    default Flowable<Record> listShardRecordsAfterSequenceNumber(String streamName, Shard shard, String startingSequenceNumber) {
        return getShardRecords(streamName, shard, ShardIteratorType.AFTER_SEQUENCE_NUMBER, startingSequenceNumber, 0);
    }

    /**
     *
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @param batchLimit
     * @return
     */
    default Flowable<Record> listShardRecordsAfterSequenceNumber(Shard shard, String startingSequenceNumber, int batchLimit) {
        return listShardRecordsAfterSequenceNumber(getDefaultStreamName(), shard, startingSequenceNumber, batchLimit);
    }

    /**
     *
     * @param shard the shard to be used
     * @param startingSequenceNumber
     * @return
     */
    default Flowable<Record> listShardRecordsAfterSequenceNumber(Shard shard, String startingSequenceNumber) {
        return listShardRecordsAfterSequenceNumber(getDefaultStreamName(), shard, startingSequenceNumber, 0);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param batchLimit
     * @return
     */
    default Flowable<Record> listShardRecordsFromHorizon(String streamName, Shard shard, int batchLimit) {
        return getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", batchLimit);
    }
    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @return
     */
    default Flowable<Record> listShardRecordsFromHorizon(String streamName, Shard shard) {
        return getShardRecords(streamName, shard, ShardIteratorType.TRIM_HORIZON, "", 0);
    }

    /**
     *
     * @param shard the shard to be used
     * @param batchLimit
     * @return
     */
    default Flowable<Record> listShardRecordsFromHorizon(Shard shard, int batchLimit) {
        return listShardRecordsFromHorizon(getDefaultStreamName(), shard, batchLimit);
    }
    /**
     *
     * @param shard the shard to be used
     * @return
     */
    default Flowable<Record> listShardRecordsFromHorizon(Shard shard) {
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
            .map(DescribeStreamResponse::streamDescription)
            .map(StreamDescription::shards)
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
    PutRecordResponse putEvent(String streamName, Event event, String sequenceNumberForOrdering);

    /**
     *
     * @param streamName the name of the stream
     * @param event
     * @return
     */
    default PutRecordResponse putEvent(String streamName, Event event) {
        return putEvent(streamName, event, "");
    }

    /**
     *
     * @param event
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResponse putEvent(Event event, String sequenceNumberForOrdering) {
        return putEvent(getDefaultStreamName(), event, sequenceNumberForOrdering);
    }

    /**
     *
     * @param event
     * @return
     */
    default PutRecordResponse putEvent(Event event) {
        return putEvent(getDefaultStreamName(), event, "");
    }

    /**
     *
     * @param streamName the name of the stream
     * @param events
     * @return
     */
    PutRecordsResponse putEvents(String streamName, List<Event> events);

    /**
     *
     * @param events
     * @return
     */
    default PutRecordsResponse putEvents(List<Event> events) {
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
    default PutRecordResponse putRecord(String streamName, String partitionKey, String data, String sequenceNumberForOrdering) {
        return putRecord(streamName, partitionKey, data.getBytes(), sequenceNumberForOrdering);
    }

    /**
     *
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResponse putRecord(String partitionKey, String data, String sequenceNumberForOrdering) {
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
    PutRecordResponse putRecord(String streamName, String partitionKey, byte[] data, String sequenceNumberForOrdering);

    /**
     *
     * @param streamName the name of the stream
     * @param partitionKey
     * @param data
     * @return
     */
    default PutRecordResponse putRecord(String streamName, String partitionKey, byte[] data) {
        return putRecord(streamName, partitionKey, data, null);
    }

    /**
     *
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    default PutRecordResponse putRecord(String partitionKey, byte[] data, String sequenceNumberForOrdering) {
        return putRecord(getDefaultStreamName(), partitionKey, data, sequenceNumberForOrdering);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param records
     * @return
     */
    PutRecordsResponse putRecords(String streamName, List<PutRecordsRequestEntry> records);

    /**
     *
     * @param records
     * @return
     */
    default PutRecordsResponse putRecords(List<PutRecordsRequestEntry> records) {
        return putRecords(getDefaultStreamName(), records);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shardId1
     * @param shardId2
     */
    MergeShardsResponse mergeShards(String streamName, String shardId1, String shardId2);

    /**
     *
     * @param shardId1
     * @param shardId2
     */
    default MergeShardsResponse mergeShards(String shardId1, String shardId2) {
        return mergeShards(getDefaultStreamName(), shardId1, shardId2);
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     * @param newStartingHashKey
     */
    SplitShardResponse splitShard(String streamName, Shard shard, String newStartingHashKey);

    /**
     *
     * @param streamName the name of the stream
     * @param shard the shard to be used
     */
    default SplitShardResponse splitShard(String streamName, Shard shard) {
        return splitShard(streamName, shard, "");
    }

    /**
     *
     * @param streamName the name of the stream
     * @param shardId the id of the shard
     */
    default SplitShardResponse splitShard(String streamName, String shardId) {
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
    default SplitShardResponse splitShard(String shardId) {
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
