package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

public interface DetachedCriteria<T> {

    Flowable<T> query(IDynamoDBMapper mapper);
    int count(IDynamoDBMapper mapper);
    DynamoDBQueryExpression<T> resolveExpression(IDynamoDBMapper mapper);

}
