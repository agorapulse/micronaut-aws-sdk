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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional.QueryConditionalFactory;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class DefaultFilterConditionCollector<T> implements FilterConditionCollector<T> {

    DefaultFilterConditionCollector(MappedTableResource<T> table, AttributeConversionHelper attributeConversionHelper) {
        this.table = table;
        this.attributeConversionHelper = attributeConversionHelper;
    }

    @Override
    public DefaultFilterConditionCollector<T> inList(String attributeOrIndex, Object... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    @Override
    public DefaultFilterConditionCollector<T> inList(String attributeOrIndex, Collection<?> values) {
        List<AttributeValue> valuesAttributes = values.stream().map(value -> attributeConversionHelper.convert(table, attributeOrIndex, value)).collect(Collectors.toList());
        conditions.add(QueryConditionalFactory.inList(attributeOrIndex, valuesAttributes));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> eq(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> ne(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.notEqualTo(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> le(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> lt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.lessThan(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> ge(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> gt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.greaterThan(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeEq(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeEqualTo(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeNe(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeNotEqualTo(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeLe(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeLessThanOrEqualTo(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeLt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeLessThan(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeGe(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeGreaterThanOrEqualTo(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> sizeGt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.sizeGreaterThan(attributeOrIndex, AttributeValues.numberValue((Number) value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> between(String attributeOrIndex, Object lo, Object hi) {
        conditions.add(QueryConditionalFactory.between(
            attributeOrIndex,
            attributeConversionHelper.convert(table, attributeOrIndex, lo),
            attributeConversionHelper.convert(table, attributeOrIndex, hi))
        );
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> notExists(String attributeOrIndex) {
        conditions.add(QueryConditionalFactory.attributeNotExists(attributeOrIndex));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> isNull(String attributeOrIndex) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.attributeExists(attributeOrIndex)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> contains(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.contains(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value)));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> notContains(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.contains(attributeOrIndex, attributeConversionHelper.convert(table, attributeOrIndex, value))));
        return this;
    }

    @Override
    public FilterConditionCollector<T> typeOf(String attributeOrIndex, Class<?> type) {
        conditions.add(QueryConditionalFactory.attributeType(attributeOrIndex, type));
        return this;
    }

    @Override
    public DefaultFilterConditionCollector<T> beginsWith(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.beginsWith(attributeOrIndex, value));
        return this;
    }

    /**
     * One or more range key filter conditions in disjunction.
     *
     *  @param conditions consumer to build the conditions
     * @return self
     */
    @Override
    public DefaultFilterConditionCollector<T> group(Consumer<FilterConditionCollector<T>> conditions) {
        DefaultFilterConditionCollector<T> nested = new DefaultFilterConditionCollector<>(table, attributeConversionHelper);
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
    public DefaultFilterConditionCollector<T> or(Consumer<FilterConditionCollector<T>> conditions) {
        DefaultFilterConditionCollector<T> nested = new DefaultFilterConditionCollector<>(table, attributeConversionHelper);
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
    public DefaultFilterConditionCollector<T> and(Consumer<FilterConditionCollector<T>> conditions) {
        DefaultFilterConditionCollector<T> nested = new DefaultFilterConditionCollector<>(table, attributeConversionHelper);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.and(nested.conditions));

        return this;
    }

    public QueryConditional getCondition() {
        return QueryConditionalFactory.and(conditions);
    }

    private final MappedTableResource<T> table;
    private final AttributeConversionHelper attributeConversionHelper;
    private List<QueryConditional> conditions = new LinkedList<>();
}
