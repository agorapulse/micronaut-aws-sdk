package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.Arrays;
import java.util.function.Consumer;

public interface QueryBuilder<T> extends DetachedQuery<T> {

    QueryBuilder<T> consistent(Builders.Read read);
    QueryBuilder<T> inconsistent(Builders.Read read);

    QueryBuilder<T> index(String name);

    QueryBuilder<T> hash(Object key);

    QueryBuilder<T> range(Consumer<RangeConditionCollector<T>> conditions);

    default QueryBuilder<T> range(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return range(ConsumerWithDelegate.create(conditions));
    }

    QueryBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    default QueryBuilder<T> filter(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    QueryBuilder<T> filter(ConditionalOperator or);

    QueryBuilder<T> page(int page);

    QueryBuilder<T> offset(Object exclusiveStartKeyValue);

    QueryBuilder<T> configure(Consumer<DynamoDBQueryExpression<T>> configurer);

    default QueryBuilder<T> configure(
        @DelegatesTo(type = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression<T>")
            Closure<Object> configurer
    ) {
        return configure(ConsumerWithDelegate.create(configurer));
    }

    QueryBuilder<T> only(Iterable<String> propertyPaths);

    default QueryBuilder<T> only(String... propertyPaths) {
        return only(Arrays.asList(propertyPaths));
    }

    default QueryBuilder<T> only(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_ONLY)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> collector
    ) {
        return only(PathCollector.collectPaths(collector).getPropertyPaths());
    }

}
