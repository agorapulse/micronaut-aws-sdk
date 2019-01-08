package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

/**
 * An interface for queries which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedQuery<T> {

    /**
     * Executes a query using provided mapper.
     * @param mapper DynamoDB mapper
     * @return flowable of entities found for the current query
     */
    Flowable<T> query(IDynamoDBMapper mapper);

    /**
     * Counts entities satisfying given query using provided mapper.
     * @param mapper DynamoDB mapper
     * @return count of entities satisfying  for the current query
     */
    int count(IDynamoDBMapper mapper);

    /**
     * Resolves the current query into native query expression using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current query resolved into native query expression
     */
    DynamoDBQueryExpression<T> resolveExpression(IDynamoDBMapper mapper);

}
