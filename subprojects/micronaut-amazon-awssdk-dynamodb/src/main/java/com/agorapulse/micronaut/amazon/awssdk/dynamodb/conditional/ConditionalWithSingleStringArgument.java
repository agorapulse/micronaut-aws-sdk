package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Collections;

class ConditionalWithSingleStringArgument implements QueryConditional {

    static final String ATTRIBUTE_EXISTS = "attribute_exists (%s)";
    static final String ATTRIBUTE_NOT_EXISTS = "attribute_not_exists (%s)";
    static final String SIZE = "size (%s)";

    private final String template;
    private final String path;

    ConditionalWithSingleStringArgument(String template, String path) {
        this.template = template;
        this.path = path;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        String pathKeyToken = QueryConditionalFactory.expressionKey(path);
        String queryExpression = String.format(template, pathKeyToken);

        return Expression.builder()
            .expression(queryExpression)
            .expressionNames(Collections.singletonMap(pathKeyToken, path))
            .build();
    }

}
