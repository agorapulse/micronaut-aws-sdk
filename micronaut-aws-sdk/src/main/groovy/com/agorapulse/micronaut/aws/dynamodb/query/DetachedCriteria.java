package com.agorapulse.micronaut.aws.dynamodb.query;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

public interface DetachedCriteria<T> {

    Flowable<T> query(IDynamoDBMapper mapper);
    long count(IDynamoDBMapper mapper);

}
