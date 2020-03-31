package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

class ConditionalWithTwoArguments implements QueryConditional {

    static final List<String> TYPES = Arrays.asList("S", "SS", "N", "NS", "B", "BS", "BOOL", "NULL", "L", "M");
    static final String EQUAL_TO_TEMPLATE = "%s = %s";
    static final String NOT_EQUAL_TO_TEMPLATE = "%s <> %s";
    static final String LESS_THAN_TEMPLATE = "%s < %s";
    static final String LESS_THAN_OR_EQUAL_TO_TEMPLATE = "%s <= %s";
    static final String GREATER_THAN_TEMPLATE = "%s > %s";
    static final String GREATER_THAN_OR_EQUAL_TO_TEMPLATE = "%s >= %s";
    static final String BEGINS_WITH_TEMPLATE = "begins_with (%s, %s)";
    static final String ATTRIBUTE_TYPE_TEMPLATE = "attribute_type (%s, %s)";
    static final String CONTAINS_TEMPLATE = "contains (%s, %s)";

    private final String template;
    private final String property;
    private final Supplier<AttributeValue> value;

    ConditionalWithTwoArguments(String template, String property, Supplier<AttributeValue> value) {
        this.template = template;
        this.property = property;
        this.value = value;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        String propertyKeyToken = QueryConditionalFactory.expressionKey(property);
        String propertyValueToken = QueryConditionalFactory.expressionValue(property);
        String queryExpression = String.format(template, propertyKeyToken, propertyValueToken);

        return Expression.builder()
            .expression(queryExpression)
            .expressionNames(Collections.singletonMap(propertyKeyToken, property))
            .expressionValues(Collections.singletonMap(propertyValueToken, value.get()))
            .build();
    }

}
