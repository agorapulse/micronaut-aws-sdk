/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional.QueryConditionalFactory;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

final class DefaultKeyConditionCollector<T> implements KeyConditionCollector<T> {

    public DefaultKeyConditionCollector(MappedTableResource<T> table, AttributeConversionHelper attributeConversionHelper, String index) {
        this.table = table;
        this.attributeConversionHelper = attributeConversionHelper;
        this.index = index;
    }

    @Override
    public DefaultKeyConditionCollector<T> eq(Object value) {
        conditions.add(QueryConditionalFactory.equalTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> le(Object value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> lt(Object value) {
        conditions.add(QueryConditionalFactory.lessThan(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> ge(Object value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> gt(Object value) {
        conditions.add(QueryConditionalFactory.greaterThan(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> between(Object lo, Object hi) {
        conditions.add(QueryConditionalFactory.between(
            getSortKey(),
            attributeConversionHelper.convert(table, getSortKey(), lo),
            attributeConversionHelper.convert(table, getSortKey(), hi))
        );
        return this;
    }

    @Override
    public DefaultKeyConditionCollector<T> beginsWith(String value) {
        conditions.add(QueryConditionalFactory.beginsWith(getSortKey(), value));
        return this;
    }

    public QueryConditional getCondition() {
        return QueryConditionalFactory.and(conditions);
    }

    private final MappedTableResource<T> table;
    private final AttributeConversionHelper attributeConversionHelper;
    private final String index;

    private List<QueryConditional> conditions = new LinkedList<>();

    String getSortKey() {
        Optional<String> sortKey = table.tableSchema().tableMetadata().indexSortKey(index);
        if (!sortKey.isPresent()) {
            throw new IllegalStateException("Sort key not defined for " + table.tableSchema().itemType() + " and index " + index);
        }
        return sortKey.get();
    }

}
