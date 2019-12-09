package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;

/**
 * An interface for updates which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedUpdate<T> {

    /**
     * Executes an update using provided mapper.
     * @param mapper DynamoDB mapper
     * @param client low level AWS SDK client
     * @return the return value which depends on the configuration of the update request
     */
    Object update(IDynamoDBMapper mapper, AmazonDynamoDB client);

    /**
     * Resolves the current update into native update request using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current update resolved into native update request
     */
    UpdateItemRequest resolveExpression(IDynamoDBMapper mapper);

}
