package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Date;

@DynamoDBTable(tableName = "entity")
public class DynamoDBEntityWithAnnotationsOnMethods {

    public static final String RANGE_INDEX = "rangeIndex";
    public static final String DATE_INDEX = "date";

    String parentId;
    String id;
    String rangeIndex;
    Date date;
    Integer number = 0;

    @DynamoDBHashKey
    public String getParentId() {
        return parentId;
    }
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @DynamoDBRangeKey
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBIndexRangeKey(localSecondaryIndexName = RANGE_INDEX)
    public String getRangeIndex() {
        return rangeIndex;
    }
    public void setRangeIndex(String rangeIndex) {
        this.rangeIndex = rangeIndex;
    }

    @DynamoDBIndexRangeKey(localSecondaryIndexName = DATE_INDEX)
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    public Integer getNumber() {
        return number;
    }
    public void setNumber(Integer number) {
        this.number = number;
    }
}
