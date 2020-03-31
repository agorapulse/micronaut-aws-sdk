package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

class LogicalConditional implements QueryConditional {

    static final String AND_TOKEN = " AND ";
    static final String OR_TOKEN = " OR ";

    private final String token;
    private final Iterable<QueryConditional> statements;

    LogicalConditional(String token, Iterable<QueryConditional> statements) {
        this.token = token;
        this.statements = statements;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        Expression.Builder builder = Expression.builder();
        List<Expression> expressions = StreamSupport.stream(statements.spliterator(), false)
            .map(s -> s.expression(tableSchema, indexName))
            .collect(Collectors.toList());
        builder.expression(expressions.stream().map(Expression::expression).collect(joining(token)));
        expressions.forEach(ex -> {
            ex.expressionNames().forEach(builder::putExpressionName);
            ex.expressionValues().forEach(builder::putExpressionValue);
        });
        return builder.build();
    }

}
