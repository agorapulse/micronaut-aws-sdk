/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.ConvertedJson;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondaryPartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondarySortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import io.micronaut.core.annotation.Introspected;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Introspected                                                                           // <1>
public class DynamoDBEntity implements PlaybookAware {

    public static final String DATE_INDEX = "date";
    public static final String RANGE_INDEX = "rangeIndex";
    public static final String GLOBAL_INDEX = "globalIndex";

    private String parentId;
    private String id;
    private String rangeIndex;
    private Date date;
    private Integer number = 0;

    private Map<String, List<String>> mapProperty = new LinkedHashMap<>();
    private Set<String> stringSetProperty = new HashSet<>();
    private Options options = new Options();

    @PartitionKey                                                                       // <2>
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @SortKey                                                                            // <3>
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SecondarySortKey(indexNames = RANGE_INDEX)                                         // <4>
    public String getRangeIndex() {
        return rangeIndex;
    }

    public void setRangeIndex(String rangeIndex) {
        this.rangeIndex = rangeIndex;
    }

    @Projection(ProjectionType.ALL)                                                     // <5>
    @SecondarySortKey(indexNames = DATE_INDEX)
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

    @Projection(ProjectionType.ALL)
    @SecondaryPartitionKey(indexNames = GLOBAL_INDEX)                                   // <6>
    public String getGlobalIndex() {
        return parentId + ":" + id;
    }

    public Map<String, List<String>> getMapProperty() {
        return mapProperty;
    }

    public void setMapProperty(Map<String, List<String>> mapProperty) {
        this.mapProperty = mapProperty;
    }

    public Set<String> getStringSetProperty() {
        return stringSetProperty;
    }

    public void setStringSetProperty(Set<String> stringSetProperty) {
        this.stringSetProperty = stringSetProperty;
    }

    @ConvertedJson
    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    //CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamoDBEntity that = (DynamoDBEntity) o;
        return Objects.equals(parentId, that.parentId) &&
            Objects.equals(id, that.id) &&
            Objects.equals(rangeIndex, that.rangeIndex) &&
            Objects.equals(date, that.date) &&
            Objects.equals(number, that.number) &&
            Objects.equals(mapProperty, that.mapProperty) &&
            Objects.equals(stringSetProperty, that.stringSetProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentId, id, rangeIndex, date, number, mapProperty, stringSetProperty);
    }

    @Override
    public String toString() {
        return "DynamoDBEntity{" +
            "parentId='" + parentId + '\'' +
            ", id='" + id + '\'' +
            ", rangeIndex='" + rangeIndex + '\'' +
            ", date=" + date +
            ", number=" + number +
            ", mapProperty=" + mapProperty +
            ", stringSetProperty=" + stringSetProperty +
            '}';
    }
    //CHECKSTYLE:ON
}
