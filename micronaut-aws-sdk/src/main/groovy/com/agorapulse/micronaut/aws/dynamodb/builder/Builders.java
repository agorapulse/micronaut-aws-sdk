package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.agorapulse.micronaut.aws.dynamodb.DefaultDynamoDBService;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public final class Builders {

    private Builders() {}

    enum Sort {
        ASC, DESC
    }

    enum Read {
        READ
    }

    public static final Sort asc = Sort.ASC;
    public static final Sort desc = Sort.DESC;
    public static final Read read = Read.READ;

    public static final ConditionalOperator or = ConditionalOperator.OR;
    public static final ConditionalOperator and = ConditionalOperator.AND;

    public static final ReturnValue none = ReturnValue.NONE;
    public static final ReturnValue allOld = ReturnValue.ALL_OLD;
    public static final ReturnValue updatedOld = ReturnValue.UPDATED_OLD;
    public static final ReturnValue allNew = ReturnValue.ALL_NEW;
    public static final ReturnValue updatedNew = ReturnValue.UPDATED_NEW;

    public static <T> QueryBuilder<T> query(Class<T> type, Consumer<QueryBuilder<T>> definition) {
        QueryBuilder<T> builder = new DefaultQueryBuilder<T>(type, new DynamoDBQueryExpression<T>().withLimit(DefaultDynamoDBService.DEFAULT_QUERY_LIMIT));
        definition.accept(builder);
        return builder;
    }

    public static <T> QueryBuilder<T> query(
        Class<T> type,
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.QueryBuilder<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.QueryBuilder<T>")
            Closure<QueryBuilder<T>> definition
    ) {
        return query(type, ConsumerWithDelegate.create(definition));
    }

    public static <T> UpdateBuilder<T> update(Class<T> type, Consumer<UpdateBuilder<T>> definition) {
        UpdateBuilder<T> builder = new DefaultUpdateBuilder<>(type);
        definition.accept(builder);
        return builder;
    }

    public static <T> UpdateBuilder<T> update(
        Class<T> type,
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.UpdateBuilder<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.UpdateBuilder<T>")
            Closure<UpdateBuilder<T>> definition
    ) {
        return update(type, ConsumerWithDelegate.create(definition));
    }

}
