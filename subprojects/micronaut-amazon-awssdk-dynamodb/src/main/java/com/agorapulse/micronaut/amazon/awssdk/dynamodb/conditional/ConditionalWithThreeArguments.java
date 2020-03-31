package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.function.Supplier;

class ConditionalWithThreeArguments implements QueryConditional {

    static final String BETWEEN_TEMPLATE = "%s BETWEEN %s AND %s";

    private final String template;
    private final String property;
    private final Supplier<AttributeValue> first;
    private final Supplier<AttributeValue> second;

    ConditionalWithThreeArguments(String template, String property, Supplier<AttributeValue> first, Supplier<AttributeValue> second) {
        this.template = template;
        this.property = property;
        this.first = first;
        this.second = second;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        String propertyKeyToken = QueryConditionalFactory.expressionKey(property);
        String firstValueToken = QueryConditionalFactory.expressionValue(property) + "_1";
        String secondValueToken = QueryConditionalFactory.expressionValue(property) + "_2";
        String queryExpression = String.format(template, propertyKeyToken, firstValueToken, secondValueToken);

        return Expression.builder()
            .expression(queryExpression)
            .expressionNames(Collections.singletonMap(propertyKeyToken, property))
            .putExpressionValue(firstValueToken, first.get())
            .putExpressionValue(secondValueToken, second.get())
            .build();
    }

}
