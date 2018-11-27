package com.agorapulse.micronaut.aws.kinesis;

/**
 * Determines how the shard iterator is used to start reading data records from the shard.</p>
 */
public enum ShardIteratorType {
    /**
     * Start reading from the position denoted by a specific sequence number, provided in the value
     */
    AT_SEQUENCE_NUMBER,

    /**
     * Start reading right after the position denoted by a specific sequence number, provided in the value
     */
    AFTER_SEQUENCE_NUMBER,

    /**
     *  Start reading from the position denoted by a specific time stamp, provided in the value
     */
    AT_TIMESTAMP,

    /**
     * Start reading at the last untrimmed record in the shard in the system, which is the oldest data record in the shard.
     */
    TRIM_HORIZON,

    /**
     * Start reading just after the most recent record in the shard, so that you always read the most recent data in the shard.
     */
    LATEST;
}
