package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface QueryBuilder<T> extends DetachedCriteria<T> {

    QueryBuilder<T> inconsistent(Builders.Read read);

    QueryBuilder<T> index(String name);

    QueryBuilder<T> hash(Object key);

    QueryBuilder<T> range(Consumer<RangeConditionCollector<T>> conditions);

    default QueryBuilder<T> range(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return range(ConsumerWithDelegate.create(conditions));
    }

    QueryBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    default QueryBuilder<T> filter(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    QueryBuilder<T> filter(ConditionalOperator or);

    QueryBuilder<T> page(int page);

    QueryBuilder<T> offset(Object exclusiveStartKeyValue);

}
