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
package com.agorapulse.micronaut.aws.kinesis

import com.amazonaws.services.kinesis.model.CreateStreamResult
import com.amazonaws.services.kinesis.model.DeleteStreamResult
import com.amazonaws.services.kinesis.model.DescribeStreamResult
import com.amazonaws.services.kinesis.model.MergeShardsResult
import com.amazonaws.services.kinesis.model.PutRecordResult
import com.amazonaws.services.kinesis.model.PutRecordsResult
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry
import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.model.Shard
import com.amazonaws.services.kinesis.model.SplitShardResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import reactor.core.publisher.Flux
import spock.lang.Retry
import spock.lang.Specification
import spock.lang.Stepwise

import jakarta.inject.Inject

/**
 * Tests for kinesis service.
 */
@Stepwise
@MicronautTest
@Property(name = 'aws.kinesis.stream', value = STREAM)
@Property(name = 'aws.kinesis.consumer-filter-key', value = 'test_')
class KinesisServiceSpec extends Specification {

    private static final String STREAM = 'STREAM'
    private static final String VALUE_1 = 'VALUE'
    private static final String VALUE_2 = 'VALUE_2'
    private static final ObjectMapper MAPPER = new ObjectMapper()
    private static final int SHARD_COUNT = 2

    @Inject KinesisService service

    void 'new default stream'() {
        when:
            CreateStreamResult result = service.createStream(SHARD_COUNT)
        then:
            result
    }

    void 'stream is creating'() {
        when:
            DescribeStreamResult stream = service.describeStream()
        then:
            stream.streamDescription.streamName == STREAM
            stream.streamDescription.streamStatus == 'CREATING'
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
            DescribeStreamResult stream = service.describeStream()
        then:
            stream.streamDescription.streamName == STREAM
            stream.streamDescription.streamStatus == 'ACTIVE'
            stream.streamDescription.shards.size() == SHARD_COUNT
    }

    void 'list shards'() {
        when:
            List<Shard> shards = service.listShards()
        then:
            shards.size() == SHARD_COUNT
    }

    void 'get shard'() {
        when:
            String shardId = service.listShards().first().shardId
            Shard shard = service.getShard(shardId)
        then:
            short
            shard.shardId == shardId
    }

    void 'put event and retieve it'() {
        when:
            TestEvent event = new TestEvent(value: VALUE_1)
            PutRecordResult result = service.putEvent(event)
        then:
            result

            Shard shard = service.getShard(result.shardId)
        when:
            Record record = Flux.from(service.getShardOldestRecord(shard)).blockFirst()
        then:
            record
            shard
            record.sequenceNumber == result.sequenceNumber
            service.decodeRecordData(record) == MAPPER.writeValueAsString(event)
        and:
            service.getShardRecordAtSequenceNumber(shard, result.sequenceNumber)
    }

    void 'put events and retieve them'() {
        when:
            TestEvent event = new TestEvent(value: VALUE_2)
            PutRecordsResult result = service.putEvents([event])
        then:
            result

        when:
            PutRecordsResultEntry firstRecord = result.records.first()
            Shard shard = service.getShard(firstRecord.shardId)
            Record record = Flux.from(service.getShardRecordAtSequenceNumber(shard, firstRecord.sequenceNumber)).blockFirst()
        then:
            record
            shard
    }

    void 'split shards'() {
        given:
            Shard shard = service.listShards().first()
        when:
            SplitShardResult result = service.splitShard(shard.shardId)
        then:
            result
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream is active after splitting'() {
        when:
            DescribeStreamResult stream = service.describeStream()
        then:
            stream.streamDescription.streamName == STREAM
            stream.streamDescription.streamStatus == 'ACTIVE'
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'shards are split'() {
        expect:
            service.listShards().size() == SHARD_COUNT * 2
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'merge shards'() {
        when:
            MergeShardsResult result = service.mergeShards(service.listShards()[2].shardId, service.listShards()[3].shardId)
        then:
            result
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'stream is active after merging'() {
        when:
            DescribeStreamResult stream = service.describeStream()
        then:
            stream.streamDescription.streamName == STREAM
            stream.streamDescription.streamStatus == 'ACTIVE'
    }

    @Retry(
        count = 10,
        delay = 100
    )
    void 'shards are merged'() {
        expect:
            service.listShards().size() == SHARD_COUNT * 2 + 1
    }

    //
    // delete stream in the end
    //

    void 'delete stream'() {
        when:
            DeleteStreamResult result = service.deleteStream()
        then:
            result
    }

    void 'stream is deleting'() {
        when:
            DescribeStreamResult stream = service.describeStream()
        then:
            stream.streamDescription.streamName == STREAM
            stream.streamDescription.streamStatus == 'DELETING'
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
