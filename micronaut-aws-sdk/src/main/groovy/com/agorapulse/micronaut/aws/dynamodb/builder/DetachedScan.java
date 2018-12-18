package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

/**
 * An interface for scans which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedScan<T> {

    /**
     * Executes a scan using provided mapper.
     * @param mapper DynamoDB mapper
     * @return flowable of entities found for the current scan
     */
    Flowable<T> scan(IDynamoDBMapper mapper);

    /**
     * Counts entities satisfying given scan using provided mapper.
     * @param mapper DynamoDB mapper
     * @return count of entities satisfying  for the current scan
     */
    int count(IDynamoDBMapper mapper);

    /**
     * Resolves the current scan into native scan expression using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current scan resolved into native scan expression
     */
    DynamoDBScanExpression resolveExpression(IDynamoDBMapper mapper);

}
