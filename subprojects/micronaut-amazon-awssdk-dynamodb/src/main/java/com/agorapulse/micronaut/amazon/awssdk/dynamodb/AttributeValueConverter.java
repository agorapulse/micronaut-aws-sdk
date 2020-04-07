package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.Map;


public interface AttributeValueConverter {

    default <T> AttributeValue convert(DynamoDbTable<T> table, String key, Object value) {
        return convert(table, Collections.singletonMap(key, value)).get(key);
    }

    <T> Map<String, AttributeValue> convert(DynamoDbTable<T> table, Map<String, Object> values);

}
