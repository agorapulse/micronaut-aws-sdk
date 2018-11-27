package com.agorapulse.micronaut.aws.kinesis


import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.reactivex.Flowable

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

@Slf4j
@CompileStatic
class DefaultKinesisService implements KinesisService {

    public static long DEFAULT_GET_RECORDS_WAIT = 1000 // wait for 1s before getting next batch
    public static int MAX_PUT_RECORDS_SIZE = 500

    CharsetDecoder decoder = Charset.forName('UTF-8').newDecoder()

    private final AmazonKinesis client
    private final KinesisConfiguration configuration
    private final ObjectMapper objectMapper

    DefaultKinesisService(AmazonKinesis client, KinesisConfiguration configuration, ObjectMapper objectMapper) {
        this.client = client
        this.configuration = configuration
        this.objectMapper = objectMapper
    }

    @Override
    String getDefaultStreamName() {
        assert configuration.stream, "Default stream must be defined"
        return configuration.stream
    }

    /**
     *
     * @param streamName
     * @param shardCount
     */
    @Override
    CreateStreamResult createStream(String streamName, int shardCount) {
        client.createStream(streamName, shardCount)
    }
    /**
     *
     * @param record
     * @return
     */
    @Override
    String decodeRecordData(Record record) {
        String data = ''
        try {
            // Payload as UTF-8 chars.
            data = decoder.decode(record.data).toString()
        } catch (CharacterCodingException e) {
            log.error "Malformed data for record: ${record}", e
        }
        data
    }

    /**
     *
     * @param streamName
     */
    @Override
    DeleteStreamResult deleteStream(String streamName) {
        client.deleteStream(streamName)
    }

    /**
     *
     * @param streamName
     * @return
     */
    @Override
    DescribeStreamResult describeStream(String streamName) {
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest(
                streamName: streamName
        )
        DescribeStreamResult describeStreamResult

        String exclusiveStartShardId
        List<Shard> shards = []
        while(true) {
            describeStreamRequest.exclusiveStartShardId = exclusiveStartShardId
            describeStreamResult = client.describeStream(describeStreamRequest)
            shards.addAll(describeStreamResult.streamDescription.shards)
            if (shards && describeStreamResult.streamDescription.hasMoreShards) {
                exclusiveStartShardId = shards[-1].shardId
            } else {
                break
            }
        }
        describeStreamResult.streamDescription.shards = shards
        describeStreamResult
    }

    /**
     *
     * @param streamName
     * @param shardId
     * @return
     */
    @Override
    Shard getShard(String streamName, String shardId) {
        DescribeStreamResult describeStreamResult = describeStream(streamName)
        if (describeStreamResult.streamDescription.shards) {
            return describeStreamResult.streamDescription.shards.find { Shard it ->
                it.shardId == shardId
            }
        }
        return null
    }

    /**
     *
     * @return
     */
    @Override
    List<String> listStreamNames() {
        client.listStreams().streamNames
    }

    /**
     *
     * @param streamName
     * @param shard
     * @param shardIteratorType
     * @param startingSequenceNumber
     * @param limit
     * @param batchLimit
     * @param maxLoopCount
     * @return
     */
    @Override
    Flowable<Record> getShardRecords(String streamName, Shard shard, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit) {
        return FlowableRecordsHelper.generate(client, streamName, shard.shardId, shardIteratorType, startingSequenceNumber, batchLimit, DEFAULT_GET_RECORDS_WAIT.intValue())
    }

    /**
     *
     * @param streamName
     * @param event
     * @param sequenceNumberForOrdering
     * @return
     */
    @Override
    PutRecordResult putEvent(String streamName, Event event, String sequenceNumberForOrdering) {
        if (configuration.consumerFilterKey) {
            event.consumerFilterKey = configuration.consumerFilterKey
        }
        putRecord(streamName, event.partitionKey, objectMapper.writeValueAsString(event), sequenceNumberForOrdering)
    }

    /**
     *
     * @param streamName
     * @param events
     * @return
     */
    @Override
    PutRecordsResult putEvents(String streamName, List<Event> events) {
        assert events.size() < MAX_PUT_RECORDS_SIZE, "Max put events size is ${MAX_PUT_RECORDS_SIZE}"
        List<PutRecordsRequestEntry> records = []
        events.each { Event event ->
            if (configuration.consumerFilterKey) {
                event.consumerFilterKey = configuration.consumerFilterKey
            }
            records << new PutRecordsRequestEntry(
                    data: ByteBuffer.wrap(objectMapper.writeValueAsString(event).bytes),
                    partitionKey: event.partitionKey
            )
        }
        putRecords(streamName, records)
    }

    /**
     *
     * @param streamName
     * @param partitionKey
     * @param data
     * @param sequenceNumberForOrdering
     * @return
     */
    @Override
    PutRecordResult putRecord(String streamName, String partitionKey, String data, String sequenceNumberForOrdering) {
        PutRecordRequest putRecordRequest = new PutRecordRequest(
                data: ByteBuffer.wrap(data.bytes),
                partitionKey: partitionKey,
                streamName: streamName
        )
        if (sequenceNumberForOrdering) {
            putRecordRequest.sequenceNumberForOrdering = sequenceNumberForOrdering
        }

        PutRecordResult putRecordResult = client.putRecord(putRecordRequest)
        putRecordResult
    }


    /**
     *
     * @param streamName
     * @param records
     * @return
     */
    @Override
    PutRecordsResult putRecords(String streamName, List<PutRecordsRequestEntry> records) {
        PutRecordsRequest putRecordsRequest = new PutRecordsRequest(
                records: records,
                streamName: streamName
        )

        client.putRecords(putRecordsRequest)
    }

    /**
     *
     * @param streamName
     * @param shardId1
     * @param shardId2
     */
    @Override
    MergeShardsResult mergeShards(String streamName, String shardId1, String shardId2) {
        MergeShardsRequest mergeShardsRequest = new MergeShardsRequest(
                streamName: streamName,
                shardToMerge: shardId1,
                adjacentShardToMerge: shardId2
        )

        client.mergeShards(mergeShardsRequest)
    }

    /**
     *
     * @param streamName
     * @param shard
     * @param newStartingHashKey
     */
    @Override
    SplitShardResult splitShard(String streamName, Shard shard, String newStartingHashKey) {
        if (!newStartingHashKey) {
            // Determine the hash key value that is half-way between the lowest and highest values in the shard
            BigInteger startingHashKey = new BigInteger(shard.hashKeyRange.startingHashKey)
            BigInteger endingHashKey = new BigInteger(shard.hashKeyRange.endingHashKey)
            newStartingHashKey = startingHashKey.add(endingHashKey).divide(new BigInteger("2")).toString()
        }

        SplitShardRequest splitShardRequest = new SplitShardRequest(
                streamName: streamName,
                shardToSplit: shard.shardId,
                newStartingHashKey: newStartingHashKey
        )

        client.splitShard(splitShardRequest)
    }

    // PRIVATE
}
