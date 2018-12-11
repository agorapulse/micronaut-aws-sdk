package com.agorapulse.micronaut.aws.dynamodb.query;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.reactivex.Flowable;

public interface DetachedQuery<T> {

    Flowable<T> execute(IDynamoDBMapper mapper);

}
