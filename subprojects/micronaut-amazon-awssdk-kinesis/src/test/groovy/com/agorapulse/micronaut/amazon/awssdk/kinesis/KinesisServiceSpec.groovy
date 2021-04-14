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
package com.agorapulse.micronaut.amazon.awssdk.kinesis

import com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse
import software.amazon.awssdk.services.kinesis.model.DeleteStreamResponse
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse
import software.amazon.awssdk.services.kinesis.model.MergeShardsResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry
import software.amazon.awssdk.services.kinesis.model.Record
import software.amazon.awssdk.services.kinesis.model.Shard
import software.amazon.awssdk.services.kinesis.model.SplitShardResponse
import software.amazon.awssdk.services.kinesis.model.StreamStatus
import spock.lang.Retry
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.environment.RestoreSystemProperties

/**
 * Tests for kinesis service.
 */
@Stepwise
@Testcontainers
@RestoreSystemProperties
class KinesisServiceSpec extends Specification {

    private static final String STREAM = 'STREAM'
    private static final String VALUE_1 = 'VALUE'
    private static final String VALUE_2 = 'VALUE_2'
    private static final ObjectMapper MAPPER = new ObjectMapper()
    private static final int SHARD_COUNT = 2

    @Shared
    LocalStackContainer localstack = new LocalStackContainer()
        .withServices(LocalStackContainer.Service.KINESIS)

    KinesisClient kinesis
    KinesisService service

    void setup() {
        // disable CBOR (not supported by Kinelite)
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), 'false')

        kinesis = KinesisClient
            .builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.KINESIS))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.accessKey, localstack.secretKey
            )))
            .region(Region.EU_WEST_1)
            .build()

        service = new DefaultKinesisService(kinesis, new DefaultKinesisConfiguration(stream: STREAM, consumerFilterKey: 'test_'), MAPPER)
    }

    @Retry
    void 'new default stream'() {
        when:
            CreateStreamResponse result = service.createStream(SHARD_COUNT)
        then:
            result
    }

    void 'stream is creating'() {
        when:
            DescribeStreamResponse stream = service.describeStream()
        then:
            stream.streamDescription().streamName() == STREAM
            stream.streamDescription().streamStatus() == StreamStatus.CREATING
    }

    void 'stream in listed streams'() {
        expect:
            STREAM in service.listStreamNames()
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream is active'() {
        when:
            DescribeStreamResponse stream = service.describeStream()
        then:
            stream.streamDescription().streamName() == STREAM
            stream.streamDescription().streamStatus() == StreamStatus.ACTIVE
            stream.streamDescription().shards().size() == SHARD_COUNT
    }

    void 'list shards'() {
        when:
            List<Shard> shards = service.listShards()
        then:
            shards.size() == SHARD_COUNT
    }

    void 'get shard'() {
        when:
            String shardId = service.listShards().first().shardId()
            Shard shard = service.getShard(shardId)
        then:
            short
            shard.shardId() == shardId
    }

    void 'put event and retieve it'() {
        when:
            TestEvent event = new TestEvent(value: VALUE_1)
            PutRecordResponse result = service.putEvent(event)
        then:
            result

            Shard shard = service.getShard(result.shardId())
        when:
            Record record = service.getShardOldestRecord(shard).blockingGet()
        then:
            record
            shard
            record.sequenceNumber() == result.sequenceNumber()
            service.decodeRecordData(record) == MAPPER.writeValueAsString(event)
        and:
            service.getShardRecordAtSequenceNumber(shard, result.sequenceNumber())
    }

    void 'put events and retieve them'() {
        when:
            TestEvent event = new TestEvent(value: VALUE_2)
            PutRecordsResponse result = service.putEvents([event])
        then:
            result

        when:
            PutRecordsResultEntry firstRecord = result.records().first()
            Shard shard = service.getShard(firstRecord.shardId())
            Record record = service.getShardRecordAtSequenceNumber(shard, firstRecord.sequenceNumber()).blockingGet()
        then:
            record
            shard
    }

    void 'split shards'() {
        given:
            Shard shard = service.listShards().first()
        when:
            SplitShardResponse result = service.splitShard(shard.shardId())
        then:
            result
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream is active after splitting'() {
        when:
            DescribeStreamResponse stream = service.describeStream()
        then:
            stream.streamDescription().streamName() == STREAM
            stream.streamDescription().streamStatus() == StreamStatus.ACTIVE
    }

    @Retry(
        count = 10,
        delay = 100
    )
    @SuppressWarnings('DuplicateNumberLiteral')
    void 'shards are split'() {
        expect:
            service.listShards().size() == SHARD_COUNT * 2
    }

    void 'merge shards'() {
        when:
            MergeShardsResponse result = service.mergeShards(service.listShards()[0].shardId(), service.listShards()[1].shardId())
        then:
            result
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream is active after merging'() {
        when:
            DescribeStreamResponse stream = service.describeStream()
        then:
            stream.streamDescription().streamName() == STREAM
            stream.streamDescription().streamStatus() == StreamStatus.ACTIVE
    }

    @Retry(
        count = 10,
        delay = 100
    )
    @SuppressWarnings('DuplicateNumberLiteral')
    void 'shards are merged'() {
        expect:
            service.listShards().size() == SHARD_COUNT * 2 + 1
    }

    //
    // delete stream in the end
    //

    void 'delete stream'() {
        when:
            DeleteStreamResponse result = service.deleteStream()
        then:
            result
    }

    void 'stream is deleting'() {
        when:
            DescribeStreamResponse stream = service.describeStream()
        then:
            stream.streamDescription().streamName() == STREAM
            stream.streamDescription().streamStatus() == StreamStatus.DELETING
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream no longer in listed streams'() {
        expect:
            !(STREAM in service.listStreamNames())
    }

}

class TestEvent extends DefaultEvent {

    String value

    @Override
    String getPartitionKey() {
        return value
    }

}
