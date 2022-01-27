/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.core.annotation.Introspected;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.Objects;

@Introspected
@DynamoDbBean
public class DynamoDBEntityNoRange {

    public static final String GLOBAL_INDEX = "globalIndex";

    String parentId;

    Integer number = 0;

    @DynamoDbPartitionKey
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

    @DynamoDbSecondaryPartitionKey(indexNames = GLOBAL_INDEX)
    public String getGlobalIndex() {
        return parentId + ":1";
    }

    public void setGlobalIndex(String globalIndex) {
        // ignore
    }

    //CHECKSTYLE:OFF
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
    //CHECKSTYLE:ON
}
