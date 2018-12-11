package com.agorapulse.micronaut.aws.dynamodb.query;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface QueryBuilder<T> extends DetachedQuery<T> {

    enum Sort {
        ASC, DESC
    }

    enum Read {
        READ
    }

    Sort asc = Sort.ASC;

    Sort desc = Sort.DESC;
    Read read = Read.READ;

    ConditionalOperator or = ConditionalOperator.OR;
    ConditionalOperator and = ConditionalOperator.AND;

    static <T> QueryBuilder<T> query(Class<T> type, Consumer<QueryBuilder<T>> definition) {
        QueryBuilder<T> builder = new DefaultQueryBuilder<>(type, new DynamoDBQueryExpression<T>());
        definition.accept(builder);
        return builder;
    }

    static <T> QueryBuilder<T> query(
        Class<T> type,
        @DelegatesTo(value = QueryBuilder.class, strategy = Closure.DELEGATE_FIRST, genericTypeIndex = 0)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.QueryBuilder<T>")
            Closure<QueryBuilder<T>> definition
    ) {
        return query(type, ConsumerWithDelegate.create(definition));
    }

    QueryBuilder<T> inconsistent(Read read);

    QueryBuilder<T> index(String name);

    QueryBuilder<T> hash(Object key);

    QueryBuilder<T> range(Consumer<RangeConditionCollector<T>> conditions);

    QueryBuilder<T> range(
        @DelegatesTo(value = RangeConditionCollector.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    );

    QueryBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    QueryBuilder<T> filter(
        @DelegatesTo(value = RangeConditionCollector.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    );

    QueryBuilder<T> filter(ConditionalOperator or);

    QueryBuilder<T> page(int page);

    QueryBuilder<T> offset(Object exclusiveStartKeyValue);

}
