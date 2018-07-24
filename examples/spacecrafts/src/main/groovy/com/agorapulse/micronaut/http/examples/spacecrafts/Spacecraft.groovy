package com.agorapulse.micronaut.http.examples.spacecrafts

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable

@DynamoDBTable(tableName = 'spacecrafts')
class Spacecraft {

    @DynamoDBHashKey
    String country

    @DynamoDBRangeKey
    String name

}
