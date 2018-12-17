package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;

public interface DetachedUpdate<T> {

    Object update(IDynamoDBMapper mapper, AmazonDynamoDB client);
    UpdateItemRequest resolveExpression(IDynamoDBMapper mapper);

}
