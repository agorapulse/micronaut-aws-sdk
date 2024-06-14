package com.agorapulse.micronaut.aws.dynamodb.convert;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.time.Instant;

public class InstantConverter implements DynamoDBTypeConverter<String, Instant> {

    public String convert(Instant instant) {
        return instant.toString();
    }

    public Instant unconvert(String date) {
        return Instant.parse(date);
    }

}
