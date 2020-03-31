package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class InListConditional implements QueryConditional {

    private final String property;
    private final List<Supplier<AttributeValue>> values;

    InListConditional(String property, List<Supplier<AttributeValue>> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be empty");
        }

        this.property = property;
        this.values = values;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        Expression.Builder builder = Expression.builder();

        String propertyKeyToken = QueryConditionalFactory.expressionKey(property);
        builder.expressionNames(Collections.singletonMap(propertyKeyToken, property));

        String valueTokenBase = QueryConditionalFactory.expressionValue(property);
        List<String> valueNames = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            String valueName = valueTokenBase + "_" + i;
            valueNames.add(valueName);
            builder.putExpressionValue(valueName, values.get(i).get());
        }

        builder.expression(propertyKeyToken + " IN  (" + String.join(", ", valueNames) + ")");

        return builder.build();
    }

}
