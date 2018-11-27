package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.*;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

import java.util.List;
import java.util.Objects;

public class FlowableRecordsHelper {

    static Flowable<Record> generate(AmazonKinesis client, String streamName, String shardId, ShardIteratorType shardIteratorType, String startingSequenceNumber, int batchLimit, int timeBetweenAttempts) {
        return Flowable.generate(() -> {
            GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest()
                .withStreamName(streamName)
                .withShardId(shardId)
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
                    Thread.sleep(timeBetweenAttempts);
                    return shardIterator;
                }
                recordEmitter.onNext(records);
                return getRecordsResult.getNextShardIterator();
            }
        }).flatMap(Flowable::fromIterable);
    }

}
