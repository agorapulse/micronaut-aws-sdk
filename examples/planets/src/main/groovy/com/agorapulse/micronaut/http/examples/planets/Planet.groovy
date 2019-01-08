package com.agorapulse.micronaut.http.examples.planets

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable

/**
 * Planet entity.
 */
@DynamoDBTable(tableName = 'planets')
class Planet {

    @DynamoDBHashKey
    String star

    @DynamoDBRangeKey
    String name

}
