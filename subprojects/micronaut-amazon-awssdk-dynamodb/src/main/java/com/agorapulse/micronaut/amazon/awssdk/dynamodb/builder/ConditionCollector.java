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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Converter;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional.QueryConditionalFactory;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ConditionCollector<T> {

    public ConditionCollector(DynamoDbTable<T> table, Converter converter) {
        this.table = table;
        this.converter = converter;
    }

    public ConditionCollector<T> eq(Object value) {
        return eq(getSortKey(), value);
    }

    public ConditionCollector<T> eq(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> ne(Object value) {
        return ne(getSortKey(), value);
    }

    public ConditionCollector<T> ne(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.notEqualTo(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> inList(Object... values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(String attributeOrIndex, Object... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    public ConditionCollector<T> inList(Collection<?> values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(String attributeOrIndex, Collection<?> values) {
        List<AttributeValue> valuesAttributes = values.stream().map(value -> converter.convert(table, attributeOrIndex, value)).collect(Collectors.toList());
        conditions.add(QueryConditionalFactory.inList(attributeOrIndex, valuesAttributes));
        return this;
    }

    public ConditionCollector<T> le(Object value) {
        return le(getSortKey(), value);
    }

    public ConditionCollector<T> le(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> lt(Object value) {
        return lt(getSortKey(), value);
    }

    public ConditionCollector<T> lt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.lessThan(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> ge(Object value) {
        return ge(getSortKey(), value);
    }

    public ConditionCollector<T> ge(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> gt(Object value) {
        return gt(getSortKey(), value);
    }

    public ConditionCollector<T> gt(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.greaterThan(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> between(Object lo, Object hi) {
        return between(getSortKey(), lo, hi);
    }

    public ConditionCollector<T> between(String attributeOrIndex, Object lo, Object hi) {
        conditions.add(QueryConditionalFactory.between(
            attributeOrIndex,
            converter.convert(table, attributeOrIndex, lo),
            converter.convert(table, attributeOrIndex, hi))
        );
        return this;
    }

    public ConditionCollector<T> isNotNull() {
        return isNotNull(getSortKey());
    }

    public ConditionCollector<T> isNotNull(String attributeOrIndex) {
        conditions.add(QueryConditionalFactory.attributeExists(attributeOrIndex));
        return this;
    }

    public ConditionCollector<T> isNull() {
        return isNull(getSortKey());
    }

    public ConditionCollector<T> isNull(String attributeOrIndex) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.attributeExists(attributeOrIndex)));
        return this;
    }

    public ConditionCollector<T> contains(Object value) {
        return contains(getSortKey(), value);
    }

    public ConditionCollector<T> contains(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.contains(attributeOrIndex, converter.convert(table, attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> notContains(Object value) {
        return notContains(getSortKey(), value);
    }

    public ConditionCollector<T> notContains(String attributeOrIndex, Object value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.contains(attributeOrIndex, converter.convert(table, attributeOrIndex, value))));
        return this;
    }

    public ConditionCollector<T> beginsWith(String value) {
        return beginsWith(getSortKey(), value);
    }

    public ConditionCollector<T> beginsWith(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.beginsWith(attributeOrIndex, value)));
        return this;
    }

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public ConditionCollector<T> group(
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>")
            Closure<ConditionCollector<T>> conditions
    ) {
        return group(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions in disjunction.
     *
     *  @param conditions consumer to build the conditions
     * @return self
     */
    public ConditionCollector<T> group(Consumer<ConditionCollector<T>> conditions) {
        ConditionCollector<T> nested = new ConditionCollector<>(table, converter);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.group(QueryConditionalFactory.and(nested.conditions)));

        return this;
    }

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public ConditionCollector<T> or(
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>")
            Closure<ConditionCollector<T>> conditions
    ) {
        return or(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions in disjunction.
     *
     *  @param conditions consumer to build the conditions
     * @return self
     */
    public ConditionCollector<T> or(Consumer<ConditionCollector<T>> conditions) {
        ConditionCollector<T> nested = new ConditionCollector<>(table, converter);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.or(nested.conditions));

        return this;
    }

    /**
     * One or more filter conditions in conjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public ConditionCollector<T> and(
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ConditionCollector<T>")
            Closure<ConditionCollector<T>> conditions
    ) {
        return and(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions in conjunction.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    public ConditionCollector<T> and(Consumer<ConditionCollector<T>> conditions) {
        ConditionCollector<T> nested = new ConditionCollector<>(table, converter);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.and(nested.conditions));

        return this;
    }

    public QueryConditional getCondition() {
        return QueryConditionalFactory.and(conditions);
    }

    private final DynamoDbTable<T> table;
    private final Converter converter;
    private List<QueryConditional> conditions = new LinkedList<>();

    String getSortKey() {
        Optional<String> sortKey = table.tableSchema().tableMetadata().primarySortKey();
        if (!sortKey.isPresent()) {
            throw new IllegalStateException("Sort key not defined for " + table.tableSchema().itemType());
        }
        return sortKey.get();
    }

    String getPartitionKey() {
        return table.tableSchema().tableMetadata().primaryPartitionKey();
    }

    AttributeValue getAttributeValue(T object, String key) {
        return table.tableSchema().attributeValue(object, key);
    }
}
