/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;

class ConditionalWithTwoArguments implements QueryConditional {

    static final String EQUAL_TO_TEMPLATE = "%s = %s";
    static final String NOT_EQUAL_TO_TEMPLATE = "%s <> %s";
    static final String LESS_THAN_TEMPLATE = "%s < %s";
    static final String LESS_THAN_OR_EQUAL_TO_TEMPLATE = "%s <= %s";
    static final String GREATER_THAN_TEMPLATE = "%s > %s";
    static final String GREATER_THAN_OR_EQUAL_TO_TEMPLATE = "%s >= %s";
    static final String BEGINS_WITH_TEMPLATE = "begins_with(%s, %s)";
    static final String ATTRIBUTE_TYPE_TEMPLATE = "attribute_type(%s, %s)";
    static final String CONTAINS_TEMPLATE = "contains(%s, %s)";

    private final String template;
    private final String property;
    private final AttributeValue value;

    ConditionalWithTwoArguments(String template, String property, AttributeValue value) {
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
            .expressionValues(Collections.singletonMap(propertyValueToken, value))
            .build();
    }

}
