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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional.QueryConditionalFactory;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.*;
import java.util.function.Consumer;

public final class ConditionCollector<T> {
    public ConditionCollector(TableSchema<T> model) {
        this.model = model;
    }

    public ConditionCollector<T> eq(String value) {
        return eq(getSortKey(), value);
    }

    public ConditionCollector<T> eq(Number value) {
        return eq(getSortKey(), value);
    }

    public ConditionCollector<T> eq(SdkBytes value) {
        return eq(getSortKey(), value);
    }

    public ConditionCollector<T> eq(AttributeValue value) {
        return eq(getSortKey(), value);
    }

    public ConditionCollector<T> eq(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> eq(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> eq(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> eq(String attributeOrIndex, AttributeValue value) {
        conditions.add(QueryConditionalFactory.equalTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ne(String value) {
        return ne(getSortKey(), value);
    }

    public ConditionCollector<T> ne(Number value) {
        return ne(getSortKey(), value);
    }

    public ConditionCollector<T> ne(SdkBytes value) {
        return ne(getSortKey(), value);
    }

    public ConditionCollector<T> ne(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.notEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ne(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.notEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ne(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.notEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> inList(String... values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(Number... values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(SdkBytes... values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(String attributeOrIndex, String... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    public ConditionCollector<T> inList(String attributeOrIndex, Number... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    public ConditionCollector<T> inList(String attributeOrIndex, SdkBytes... values) {
        return inList(attributeOrIndex, Arrays.asList(values));
    }

    public ConditionCollector<T> inList(Collection<?> values) {
        return inList(getSortKey(), values);
    }

    public ConditionCollector<T> inList(String attributeOrIndex, Collection<?> values) {
        conditions.add(QueryConditionalFactory.inList(attributeOrIndex, values));
        return this;
    }

    public ConditionCollector<T> le(String value) {
        return le(getSortKey(), value);
    }

    public ConditionCollector<T> le(Number value) {
        return le(getSortKey(), value);
    }

    public ConditionCollector<T> le(SdkBytes value) {
        return le(getSortKey(), value);
    }

    public ConditionCollector<T> le(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> le(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> le(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.lessThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> lt(String value) {
        return lt(getSortKey(), value);
    }

    public ConditionCollector<T> lt(Number value) {
        return lt(getSortKey(), value);
    }

    public ConditionCollector<T> lt(SdkBytes value) {
        return lt(getSortKey(), value);
    }

    public ConditionCollector<T> lt(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.lessThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> lt(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.lessThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> lt(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.lessThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ge(String value) {
        return ge(getSortKey(), value);
    }

    public ConditionCollector<T> ge(Number value) {
        return ge(getSortKey(), value);
    }

    public ConditionCollector<T> ge(SdkBytes value) {
        return ge(getSortKey(), value);
    }

    public ConditionCollector<T> ge(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ge(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> ge(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.greaterThanOrEqualTo(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> gt(String value) {
        return gt(getSortKey(), value);
    }

    public ConditionCollector<T> gt(Number value) {
        return gt(getSortKey(), value);
    }

    public ConditionCollector<T> gt(SdkBytes value) {
        return gt(getSortKey(), value);
    }

    public ConditionCollector<T> gt(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.greaterThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> gt(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.greaterThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> gt(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.greaterThan(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> between(String lo, String hi) {
        return between(getSortKey(), lo, hi);
    }

    public ConditionCollector<T> between(Number lo, Number hi) {
        return between(getSortKey(), lo, hi);
    }

    public ConditionCollector<T> between(SdkBytes lo, SdkBytes hi) {
        return between(getSortKey(), lo, hi);
    }

    public ConditionCollector<T> between(String attributeOrIndex, String lo, String hi) {
        conditions.add(QueryConditionalFactory.between(attributeOrIndex, lo, hi));
        return this;
    }

    public ConditionCollector<T> between(String attributeOrIndex, Number lo, Number hi) {
        conditions.add(QueryConditionalFactory.between(attributeOrIndex, lo, hi));
        return this;
    }

    public ConditionCollector<T> between(String attributeOrIndex, SdkBytes lo, SdkBytes hi) {
        conditions.add(QueryConditionalFactory.between(attributeOrIndex, lo, hi));
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

    public ConditionCollector<T> contains(String value) {
        return contains(getSortKey(), value);
    }

    public ConditionCollector<T> contains(Number value) {
        return contains(getSortKey(), value);
    }

    public ConditionCollector<T> contains(SdkBytes value) {
        return contains(getSortKey(), value);
    }

    public ConditionCollector<T> contains(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.contains(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> contains(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.contains(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> contains(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.contains(attributeOrIndex, value));
        return this;
    }

    public ConditionCollector<T> notContains(String value) {
        return notContains(getSortKey(), value);
    }

    public ConditionCollector<T> notContains(Number value) {
        return notContains(getSortKey(), value);
    }

    public ConditionCollector<T> notContains(SdkBytes value) {
        return notContains(getSortKey(), value);
    }

    public ConditionCollector<T> notContains(String attributeOrIndex, String value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.contains(attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> notContains(String attributeOrIndex, Number value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.contains(attributeOrIndex, value)));
        return this;
    }

    public ConditionCollector<T> notContains(String attributeOrIndex, SdkBytes value) {
        conditions.add(QueryConditionalFactory.not(QueryConditionalFactory.contains(attributeOrIndex, value)));
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
        ConditionCollector<T> nested = new ConditionCollector<>(model);
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
        ConditionCollector<T> nested = new ConditionCollector<>(model);
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
        ConditionCollector<T> nested = new ConditionCollector<>(model);
        conditions.accept(nested);

        this.conditions.add(QueryConditionalFactory.and(nested.conditions));

        return this;
    }

    public QueryConditional getCondition() {
        return QueryConditionalFactory.and(conditions);
    }

    private final TableSchema<T> model;
    private List<QueryConditional> conditions = new LinkedList<>();

    String getSortKey() {
        Optional<String> sortKey = model.tableMetadata().primarySortKey();
        if (!sortKey.isPresent()) {
            throw new IllegalStateException("Sort key not defined for " + model.itemType());
        }
        return sortKey.get();
    }

    String getPartitionKey() {
        return model.tableMetadata().primaryPartitionKey();
    }

    AttributeValue getAttributeValue(T object, String key) {
        return model.attributeValue(object, key);
    }
}
