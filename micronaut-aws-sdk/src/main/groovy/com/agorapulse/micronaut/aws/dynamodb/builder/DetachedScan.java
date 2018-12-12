package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

public interface DetachedScan<T> {

    Flowable<T> scan(IDynamoDBMapper mapper);
    int count(IDynamoDBMapper mapper);
    DynamoDBScanExpression resolveExpression(IDynamoDBMapper mapper);

}
