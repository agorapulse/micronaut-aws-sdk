package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

class NotConditional implements QueryConditional {

    private final QueryConditional statement;

    NotConditional(QueryConditional statement) {
        this.statement = statement;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        Expression expression = statement.expression(tableSchema, indexName);
        return Expression.builder()
            .expression(String.format(expression.expression().trim().startsWith("(") ? "NOT %s" : "NOT (%s)", expression.expression()))
            .expressionNames(expression.expressionNames())
            .expressionValues(expression.expressionValues())
            .build();
    }

}
