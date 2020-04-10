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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

final class DefaultRangeConditionCollector<T> implements RangeConditionCollector<T> {

    public DefaultRangeConditionCollector(DynamoDbTable<T> table, AttributeConversionHelper attributeConversionHelper, String index) {
        this.table = table;
        this.attributeConversionHelper = attributeConversionHelper;
        this.index = index;
    }

    @Override
    public DefaultRangeConditionCollector<T> eq(Object value) {
        conditions.add(QueryConditionalFactory.equalTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> le(Object value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> lt(Object value) {
        conditions.add(QueryConditionalFactory.lessThan(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> ge(Object value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> gt(Object value) {
        conditions.add(QueryConditionalFactory.greaterThan(getSortKey(), attributeConversionHelper.convert(table, getSortKey(), value)));
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> between(Object lo, Object hi) {
        conditions.add(QueryConditionalFactory.between(
            getSortKey(),
            attributeConversionHelper.convert(table, getSortKey(), lo),
            attributeConversionHelper.convert(table, getSortKey(), hi))
        );
        return this;
    }

    @Override
    public DefaultRangeConditionCollector<T> beginsWith(String value) {
        conditions.add(QueryConditionalFactory.beginsWith(getSortKey(), value));
        return this;
    }


    /**
     * One or more range key filter conditions in disjunction.
     *
     *  @param conditions consumer to build the conditions
     * @return self
     */
    @Override
    public DefaultRangeConditionCollector<T> group(Consumer<RangeConditionCollector<T>> conditions) {
        DefaultRangeConditionCollector<T> nested = new DefaultRangeConditionCollector<>(table, attributeConversionHelper, index);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.group(QueryConditionalFactory.and(nested.conditions)));

        return this;
    }

    /**
     * One or more range key filter conditions in disjunction.
     *
     *  @param conditions consumer to build the conditions
     * @return self
     */
    @Override
    public DefaultRangeConditionCollector<T> or(Consumer<RangeConditionCollector<T>> conditions) {
        DefaultRangeConditionCollector<T> nested = new DefaultRangeConditionCollector<>(table, attributeConversionHelper, index);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.or(nested.conditions));

        return this;
    }

    /**
     * One or more range key filter conditions in conjunction.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    @Override
    public DefaultRangeConditionCollector<T> and(Consumer<RangeConditionCollector<T>> conditions) {
        DefaultRangeConditionCollector<T> nested = new DefaultRangeConditionCollector<>(table, attributeConversionHelper, index);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.and(nested.conditions));

        return this;
    }

    public QueryConditional getCondition() {
        return QueryConditionalFactory.and(conditions);
    }

    private final DynamoDbTable<T> table;
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
