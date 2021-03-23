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
package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.util.StringUtils;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class DefaultKinesisService implements KinesisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisService.class);

    public DefaultKinesisService(AmazonKinesis client, KinesisConfiguration configuration, ObjectMapper objectMapper) {
        this.client = client;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getDefaultStreamName() {
        if (StringUtils.isEmpty(configuration.getStream())) {
            throw new IllegalStateException("Default stream must be defined");
        }

        return configuration.getStream();
    }

    @Override
    public CreateStreamResult createStream(String streamName, int shardCount) {
        try {
            return client.createStream(streamName, shardCount);
        } catch (ResourceInUseException ignored) {
            return new CreateStreamResult();
        }
    }

    @Override
    public String decodeRecordData(final Record record) {
        String data = "";
        try {
            // Payload as UTF-8 chars.
            data = decoder.decode(record.getData()).toString();
        } catch (CharacterCodingException e) {
            LOGGER.error("Malformed data for record: " + record, e);
        }

        return data;
    }

    @Override
    public DeleteStreamResult deleteStream(String streamName) {
        return client.deleteStream(streamName);
    }

    @Override
    public DescribeStreamResult describeStream(String streamName) {
        DescribeStreamRequest request = new DescribeStreamRequest().withStreamName(streamName);
        DescribeStreamResult describeStreamResult;

        String exclusiveStartShardId = null;
        List<Shard> shards = new ArrayList<>();
        while (true) {
            request.setExclusiveStartShardId(exclusiveStartShardId);
            describeStreamResult = client.describeStream(request);
            shards.addAll(describeStreamResult.getStreamDescription().getShards());
            if (!shards.isEmpty() && describeStreamResult.getStreamDescription().getHasMoreShards()) {
                exclusiveStartShardId = shards.get(shards.size() - 1).getShardId();
            } else {
                break;
            }
        }

        describeStreamResult.getStreamDescription().setShards(shards);
        return describeStreamResult;
    }

    @Override
    public Shard getShard(String streamName, final String shardId) {
        DescribeStreamResult describeStreamResult = describeStream(streamName);
        if (!describeStreamResult.getStreamDescription().getShards().isEmpty()) {
            Optional<Shard> shard = describeStreamResult
                .getStreamDescription()
                .getShards()
                .stream()
                .filter(it -> it.getShardId().equals(shardId))
                .findFirst();
            if (shard.isPresent()) {
                return shard.get();
            }
        }

        return null;
    }

    @Override
    public List<String> listStreamNames() {
        return client.listStreams().getStreamNames();
    }

    @Override
    public Flowable<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit) {
        return Flowable.generate(() -> {
            GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest()
                .withStreamName(streamName)
                .withShardId(shard.getShardId())
                .withShardIteratorType(shardIteratorType.toString());
            if (shardIteratorType == ShardIteratorType.AFTER_SEQUENCE_NUMBER || shardIteratorType == ShardIteratorType.AT_SEQUENCE_NUMBER) {
                if (startingSequenceNumber == null || startingSequenceNumber.length() == 0) {
                    throw new IllegalArgumentException("Starting sequence number must not be null!");
                }
                getShardIteratorRequest.withStartingSequenceNumber(Objects.requireNonNull(startingSequenceNumber));
            }

            GetShardIteratorResult getShardIteratorResult = client.getShardIterator(getShardIteratorRequest);
            return getShardIteratorResult.getShardIterator();
        }, new BiFunction<String, Emitter<List<Record>>, String>() {
            @Override
            public String apply(String shardIterator, Emitter<List<Record>> recordEmitter) throws Exception {
                GetRecordsRequest getRecordsRequest = new GetRecordsRequest().withShardIterator(shardIterator);

                if (batchLimit > 0) {
                    getRecordsRequest.withLimit(batchLimit);
                }

                GetRecordsResult getRecordsResult = client.getRecords(getRecordsRequest);

                List<Record> records = getRecordsResult.getRecords();
                if (records.isEmpty()) {
                    // TODO: is there better way how to wait?
                    Thread.sleep(DEFAULT_GET_RECORDS_WAIT);
                    return shardIterator;
                }
                recordEmitter.onNext(records);
                return getRecordsResult.getNextShardIterator();
            }
        }).flatMap(Flowable::fromIterable);
    }

    @Override
    public PutRecordResult putEvent(String streamName, Event event, String sequenceNumberForOrdering) {
        if (!StringUtils.isEmpty(configuration.getConsumerFilterKey())) {
            event.setConsumerFilterKey(configuration.getConsumerFilterKey());
        }

        try {
            return putRecord(streamName, event.getPartitionKey(), objectMapper.writeValueAsString(event), sequenceNumberForOrdering);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write value as JSON: " + event, e);
        }
    }

    @Override
    public PutRecordsResult putEvents(String streamName, List<Event> events) {
        if (events.size() > MAX_PUT_RECORDS_SIZE) {
            throw new IllegalArgumentException("Max put events size is " + MAX_PUT_RECORDS_SIZE);
        }
        return putRecords(streamName, events.stream().map(event -> {
            if (!StringUtils.isEmpty(configuration.getConsumerFilterKey())) {
                event.setConsumerFilterKey(configuration.getConsumerFilterKey());
            }

            try {
                ByteBuffer data = ByteBuffer.wrap(objectMapper.writeValueAsString(event).getBytes());
                return new PutRecordsRequestEntry()
                    .withData(data)
                    .withPartitionKey(event.getPartitionKey());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot write value as JSON: " + event, e);
            }
        }).collect(Collectors.toList()));
    }

    @Override
    public PutRecordResult putRecord(String streamName, String partitionKey, byte[] data, String sequenceNumberForOrdering) {
        PutRecordRequest request = new PutRecordRequest()
            .withData(ByteBuffer.wrap(data))
            .withPartitionKey(partitionKey)
            .withStreamName(streamName);

        if (!StringUtils.isEmpty(sequenceNumberForOrdering)) {
            request.withSequenceNumberForOrdering(sequenceNumberForOrdering);
        }

        return client.putRecord(request);
    }

    @Override
    public PutRecordsResult putRecords(String streamName, List<PutRecordsRequestEntry> records) {
        return client.putRecords(new PutRecordsRequest().withRecords(records).withStreamName(streamName));
    }

    @Override
    public MergeShardsResult mergeShards(String streamName, String shardId1, String shardId2) {
        return client.mergeShards(new MergeShardsRequest().withStreamName(streamName).withShardToMerge(shardId1).withAdjacentShardToMerge(shardId2));
    }

    @Override
    public SplitShardResult splitShard(String streamName, Shard shard, String newStartingHashKey) {
        if (StringUtils.isEmpty(newStartingHashKey)) {
            // Determine the hash key value that is half-way between the lowest and highest values in the shard
            BigInteger startingHashKey = new BigInteger(shard.getHashKeyRange().getStartingHashKey());
            BigInteger endingHashKey = new BigInteger(shard.getHashKeyRange().getEndingHashKey());
            newStartingHashKey = startingHashKey.add(endingHashKey).divide(BigInteger.valueOf(2)).toString();
        }

        return client.splitShard(new SplitShardRequest()
            .withStreamName(streamName)
            .withShardToSplit(shard.getShardId())
            .withNewStartingHashKey(newStartingHashKey));
    }

    @Override
    public void waitForStatus(String streamName, StreamStatus status) {
        while (!status.name().equals(describeStream(streamName).getStreamDescription().getStreamStatus())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Waiting for stream to become active was interrupted!", e);
            }
        }
    }

    private static final int DEFAULT_GET_RECORDS_WAIT = 1000;
    private static final int MAX_PUT_RECORDS_SIZE = 500;

    private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    private final AmazonKinesis client;
    private final KinesisConfiguration configuration;
    private final ObjectMapper objectMapper;
}
