package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

import java.util.Date;
import java.util.Objects;

@DynamoDBTable(tableName = "entityNoRange")
public class DynamoDBEntityNoRange {

    public static final String GLOBAL_INDEX = "globalIndex";

    @DynamoDBHashKey
    String parentId;

    Integer number = 0;

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = GLOBAL_INDEX)
    public String getGlobalIndex() {
        return parentId + ":1";
    }

    public void setGlobalIndex(String globalIndex) {
        // ignore
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamoDBEntityNoRange that = (DynamoDBEntityNoRange) o;
        return Objects.equals(parentId, that.parentId) &&
            Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, number);
    }

    @Override
    public String toString() {
        return "DynamoDBEntity{" +
            "parentId='" + parentId + '\'' +
            ", number=" + number +
            '}';
    }
}
