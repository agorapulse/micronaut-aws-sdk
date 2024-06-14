package com.agorapulse.micronaut.aws.dynamodb;

import com.agorapulse.micronaut.aws.dynamodb.convert.InstantConverter;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;

import java.time.Instant;

@DynamoDBDocument
public class LogEntry {

    public static LogEntry create(String message) {
        LogEntry entry = new LogEntry();
        entry.timestamp = Instant.now();
        entry.message = message;
        return entry;
    }

    @DynamoDBTypeConverted(converter = InstantConverter.class)
    private Instant timestamp;
    private String message;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
