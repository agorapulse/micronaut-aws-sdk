package com.agorapulse.micronaut.aws.kinesis;

import java.util.Date;
import java.util.UUID;

public interface Event {

    default String getPartitionKey() {
        return UUID.randomUUID().toString();
    }

    Date getTimestamp();
    String getConsumerFilterKey();
    void setConsumerFilterKey(String key);
}
