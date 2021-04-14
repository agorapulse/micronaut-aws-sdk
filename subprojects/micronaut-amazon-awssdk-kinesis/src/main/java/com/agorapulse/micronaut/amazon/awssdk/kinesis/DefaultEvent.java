package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * Base for event classes.
 */
public class DefaultEvent implements Event {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getConsumerFilterKey() {
        return consumerFilterKey;
    }

    public void setConsumerFilterKey(String consumerFilterKey) {
        this.consumerFilterKey = consumerFilterKey;
    }

    private Date timestamp = new Date();
    private String consumerFilterKey = "";
}
