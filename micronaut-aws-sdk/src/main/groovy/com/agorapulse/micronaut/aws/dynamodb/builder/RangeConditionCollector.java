package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel;
import com.amazonaws.services.dynamodbv2.model.Condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RangeConditionCollector<T> {
    public RangeConditionCollector(DynamoDBMapperTableModel<T> model) {
        this.model = model;
    }

    public RangeConditionCollector<T> eq(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().eq(value));
        return this;
    }

    public RangeConditionCollector<T> eq(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).eq(value));
        return this;
    }

    public RangeConditionCollector<T> ne(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().ne(value));
        return this;
    }

    public RangeConditionCollector<T> ne(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).ne(value));
        return this;
    }

    public RangeConditionCollector<T> inList(Object... values) {
        conditions.put(model.rangeKey().name(), model.rangeKey().in(values));
        return this;
    }

    public RangeConditionCollector<T> inList(String attributeOrIndex, Object... values) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).in(values));
        return this;
    }

    public RangeConditionCollector<T> inList(Collection<Object> values) {
        conditions.put(model.rangeKey().name(), model.rangeKey().in(values));
        return this;
    }

    public RangeConditionCollector<T> inList(String attributeOrIndex, Collection<Object> values) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).in(values));
        return this;
    }

    public RangeConditionCollector<T> le(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().le(value));
        return this;
    }

    public RangeConditionCollector<T> le(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).le(value));
        return this;
    }

    public RangeConditionCollector<T> lt(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().lt(value));
        return this;
    }

    public RangeConditionCollector<T> lt(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).lt(value));
        return this;
    }

    public RangeConditionCollector<T> ge(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().ge(value));
        return this;
    }

    public RangeConditionCollector<T> ge(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).ge(value));
        return this;
    }

    public RangeConditionCollector<T> gt(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().gt(value));
        return this;
    }

    public RangeConditionCollector<T> gt(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).gt(value));
        return this;
    }

    public RangeConditionCollector<T> between(Object lo, Object hi) {
        conditions.put(model.rangeKey().name(), model.rangeKey().between(lo, hi));
        return this;
    }

    public RangeConditionCollector<T> between(String attributeOrIndex, Object lo, Object hi) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).between(lo, hi));
        return this;
    }

    public RangeConditionCollector<T> isNotNull() {
        conditions.put(model.rangeKey().name(), model.rangeKey().notNull());
        return this;
    }

    public RangeConditionCollector<T> isNotNull(String attributeOrIndex) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).notNull());
        return this;
    }

    public RangeConditionCollector<T> isNull() {
        conditions.put(model.rangeKey().name(), model.rangeKey().isNull());
        return this;
    }

    public RangeConditionCollector<T> isNull(String attributeOrIndex) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).isNull());
        return this;
    }

    public RangeConditionCollector<T> contains(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().contains(value));
        return this;
    }

    public RangeConditionCollector<T> contains(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).contains(value));
        return this;
    }

    public RangeConditionCollector<T> notContains(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().notContains(value));
        return this;
    }

    public RangeConditionCollector<T> notContains(String attributeOrIndex, Object value) {
        conditions.put(attributeOrIndex, model.field(attributeOrIndex).notContains(value));
        return this;
    }

    public RangeConditionCollector<T> beginsWith(Object value) {
        conditions.put(model.rangeKey().name(), model.rangeKey().beginsWith(value));
        return this;
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
