/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.util.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.MergeShardsRequest;
import software.amazon.awssdk.services.kinesis.model.MergeShardsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceInUseException;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
import software.amazon.awssdk.services.kinesis.model.SplitShardResponse;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

import java.math.BigInteger;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class DefaultKinesisService implements KinesisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKinesisService.class);

    private static final int DEFAULT_GET_RECORDS_WAIT = 1000;
    private static final int MAX_PUT_RECORDS_SIZE = 500;

    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    private final KinesisClient client;
    private final KinesisConfiguration configuration;
    private final ObjectMapper objectMapper;

    public DefaultKinesisService(KinesisClient client, KinesisConfiguration configuration, ObjectMapper objectMapper) {
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
    public CreateStreamResponse createStream(String streamName, int shardCount) {
        try {
            return client.createStream(c -> c.streamName(streamName).shardCount(shardCount));
        } catch (ResourceInUseException ignored) {
            return CreateStreamResponse.builder().build();
        }
    }

    @Override
    public String decodeRecordData(final Record record) {
        String data = "";
        try {
            // Payload as UTF-8 chars.
            data = decoder.decode(record.data().asByteBuffer()).toString();
        } catch (CharacterCodingException e) {
            LOGGER.error("Malformed data for record: " + record, e);
        }

        return data;
    }

    @Override
    public DeleteStreamResponse deleteStream(String streamName) {
        return client.deleteStream(c -> c.streamName(streamName));
    }

    @Override
    public DescribeStreamResponse describeStream(String streamName) {
        DescribeStreamRequest.Builder request = DescribeStreamRequest.builder().streamName(streamName);
        DescribeStreamResponse describeStreamResult;

        String exclusiveStartShardId = null;
        List<Shard> shards = new ArrayList<>();
        while (true) {
            request.exclusiveStartShardId(exclusiveStartShardId);
            describeStreamResult = client.describeStream(request.build());
            shards.addAll(describeStreamResult.streamDescription().shards());
            if (!shards.isEmpty() && describeStreamResult.streamDescription().hasMoreShards() == Boolean.TRUE) {
                exclusiveStartShardId = shards.get(shards.size() - 1).shardId();
            } else {
                break;
            }
        }

        return describeStreamResult.toBuilder().streamDescription(
            describeStreamResult.streamDescription().toBuilder().shards(shards).build()
        ).build();
    }

    @Override
    public Shard getShard(String streamName, final String shardId) {
        DescribeStreamResponse describeStreamResult = describeStream(streamName);
        if (!describeStreamResult.streamDescription().shards().isEmpty()) {
            Optional<Shard> shard = describeStreamResult
                .streamDescription()
                .shards()
                .stream()
                .filter(it -> it.shardId().equals(shardId))
                .findFirst();
            if (shard.isPresent()) {
                return shard.get();
            }
        }

        return null;
    }

    @Override
    public List<String> listStreamNames() {
        return client.listStreams().streamNames();
    }

    @Override
    public Publisher<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit) {
        return Flux
            .generate(() -> {
                GetShardIteratorRequest.Builder getShardIteratorRequest = GetShardIteratorRequest.builder()
                    .streamName(streamName)
                    .shardId(shard.shardId())
                    .shardIteratorType(shardIteratorType.toString());
                if (shardIteratorType == ShardIteratorType.AFTER_SEQUENCE_NUMBER || shardIteratorType == ShardIteratorType.AT_SEQUENCE_NUMBER) {
                    if (startingSequenceNumber == null || startingSequenceNumber.length() == 0) {
                        throw new IllegalArgumentException("Starting sequence number must not be null!");
                    }
                    getShardIteratorRequest.startingSequenceNumber(Objects.requireNonNull(startingSequenceNumber));
                }

                GetShardIteratorResponse getShardIteratorResult = client.getShardIterator(getShardIteratorRequest.build());
                return getShardIteratorResult.shardIterator();
            }, (String shardIterator, SynchronousSink<List<Record>> recordEmitter) -> {
                GetRecordsRequest.Builder getRecordsRequest = GetRecordsRequest.builder().shardIterator(shardIterator);

                if (batchLimit > 0) {
                    getRecordsRequest.limit(batchLimit);
                }

                GetRecordsResponse getRecordsResult = client.getRecords(getRecordsRequest.build());

                List<Record> records = getRecordsResult.records();
                if (records.isEmpty()) {
                    return shardIterator;
                }
                recordEmitter.next(records);
                return getRecordsResult.nextShardIterator();
            })
            .delayUntil(records -> {
                if (records.isEmpty()) {
                    return Mono.just(records).delayElement(Duration.ofMillis(DEFAULT_GET_RECORDS_WAIT));
                }

                return Mono.just(records);
            })
            .flatMap(Flux::fromIterable);
    }

    @Override
    public PutRecordResponse putEvent(String streamName, Event event, String sequenceNumberForOrdering) {
        overideConsumerFilterKeyIfEmpty(event, configuration.getConsumerFilterKey());

        try {
            return putRecord(streamName, event.getPartitionKey(), objectMapper.writeValueAsString(event), sequenceNumberForOrdering);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot write value as JSON: " + event, e);
        }
    }

    @Override
    public PutRecordsResponse putEvents(String streamName, List<Event> events) {
        if (events.size() > MAX_PUT_RECORDS_SIZE) {
            throw new IllegalArgumentException("Max put events size is " + MAX_PUT_RECORDS_SIZE);
        }
        return putRecords(streamName, events.stream().map(event -> {
            overideConsumerFilterKeyIfEmpty(event, configuration.getConsumerFilterKey());

            try {
                return PutRecordsRequestEntry.builder()
                    .data(SdkBytes.fromByteArray(objectMapper.writeValueAsString(event).getBytes()))
                    .partitionKey(event.getPartitionKey())
                    .build();
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot write value as JSON: " + event, e);
            }
        }).collect(Collectors.toList()));
    }

    @Override
    public PutRecordResponse putRecord(String streamName, String partitionKey, byte[] data, String sequenceNumberForOrdering) {
        PutRecordRequest.Builder request = PutRecordRequest.builder()
            .data(SdkBytes.fromByteArray(data))
            .partitionKey(partitionKey)
            .streamName(streamName);

        if (!StringUtils.isEmpty(sequenceNumberForOrdering)) {
            request.sequenceNumberForOrdering(sequenceNumberForOrdering);
        }

        return client.putRecord(request.build());
    }

    @Override
    public PutRecordsResponse putRecords(String streamName, List<PutRecordsRequestEntry> records) {
        return client.putRecords(PutRecordsRequest.builder().records(records).streamName(streamName).build());
    }

    @Override
    public MergeShardsResponse mergeShards(String streamName, String shardId1, String shardId2) {
        return client.mergeShards(MergeShardsRequest.builder().streamName(streamName).shardToMerge(shardId1).adjacentShardToMerge(shardId2).build());
    }

    @Override
    public SplitShardResponse splitShard(String streamName, Shard shard, String newStartingHashKey) {
        if (StringUtils.isEmpty(newStartingHashKey)) {
            // Determine the hash key value that is half-way between the lowest and highest values in the shard
            BigInteger startingHashKey = new BigInteger(shard.hashKeyRange().startingHashKey());
            BigInteger endingHashKey = new BigInteger(shard.hashKeyRange().endingHashKey());
            newStartingHashKey = startingHashKey.add(endingHashKey).divide(BigInteger.valueOf(2)).toString();
        }

        return client.splitShard(SplitShardRequest.builder()
            .streamName(streamName)
            .shardToSplit(shard.shardId())
            .newStartingHashKey(newStartingHashKey).build());
    }

    @Override
    public void waitForStatus(String streamName, StreamStatus status) {
        while (!status.equals(describeStream(streamName).streamDescription().streamStatus())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Waiting for stream to become active was interrupted!", e);
            }
        }
    }

    private void overideConsumerFilterKeyIfEmpty(Event event, String consumerFilterKey) {
        if (StringUtils.isEmpty(event.getConsumerFilterKey()) && !StringUtils.isEmpty(consumerFilterKey)) {
            event.setConsumerFilterKey(consumerFilterKey);
        }
    }
}
