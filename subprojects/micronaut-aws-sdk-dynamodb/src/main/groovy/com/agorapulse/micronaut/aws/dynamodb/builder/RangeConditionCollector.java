/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel;
import com.amazonaws.services.dynamodbv2.model.Condition;

import java.util.*;

public final class RangeConditionCollector<T> {
    public RangeConditionCollector(DynamoDBMapperTableModel<T> model) {
        this.model = model;
    }

    public RangeConditionCollector<T> eq(Object value) {
        return eq(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> eq(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).eq(value));
        return this;
    }

    public RangeConditionCollector<T> ne(Object value) {
        return ne(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> ne(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).ne(value));
        return this;
    }

    public RangeConditionCollector<T> inListOf(Object... values) {
        return inList(model.rangeKey().name(), values);
    }

    public RangeConditionCollector<T> inList(String attributeOrIndex, Object... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    public RangeConditionCollector<T> inList(Collection<Object> values) {
        return inList(model.rangeKey().name(), values);
    }

    public RangeConditionCollector<T> inList(String attributeOrIndex, Collection<Object> values) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).in(values));
        return this;
    }

    public RangeConditionCollector<T> le(Object value) {
        return le(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> le(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).le(value));
        return this;
    }

    public RangeConditionCollector<T> lt(Object value) {
        return lt(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> lt(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).lt(value));
        return this;
    }

    public RangeConditionCollector<T> ge(Object value) {
        return ge(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> ge(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).ge(value));
        return this;
    }

    public RangeConditionCollector<T> gt(Object value) {
        return gt(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> gt(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).gt(value));
        return this;
    }

    public RangeConditionCollector<T> between(Object lo, Object hi) {
        return between(model.rangeKey().name(), lo, hi);
    }

    public RangeConditionCollector<T> between(String attributeOrIndex, Object lo, Object hi) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).between(lo, hi));
        return this;
    }

    public RangeConditionCollector<T> isNotNull() {
        return isNotNull(model.rangeKey().name());
    }

    public RangeConditionCollector<T> isNotNull(String attributeOrIndex) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).notNull());
        return this;
    }

    public RangeConditionCollector<T> isNull() {
        return isNull(model.rangeKey().name());
    }

    public RangeConditionCollector<T> isNull(String attributeOrIndex) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).isNull());
        return this;
    }

    public RangeConditionCollector<T> contains(Object value) {
        return contains(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> contains(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).contains(value));
        return this;
    }

    public RangeConditionCollector<T> notContains(Object value) {
        return notContains(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> notContains(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).notContains(value));
        return this;
    }

    public RangeConditionCollector<T> beginsWith(Object value) {
        return beginsWith(model.rangeKey().name(), value);
    }

    public RangeConditionCollector<T> beginsWith(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).beginsWith(value));
        return this;
    }

    public Map<String, Condition> getConditions() {
        return Collections.unmodifiableMap(conditions);
    }

    private final DynamoDBMapperTableModel<T> model;
    private Map<String, Condition> conditions = new LinkedHashMap<>();
}
