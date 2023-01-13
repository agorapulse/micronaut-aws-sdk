/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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

class ConditionalWithThreeArguments implements QueryConditional {

    static final String BETWEEN_TEMPLATE = "%s BETWEEN %s AND %s";

    private final String template;
    private final String property;
    private final AttributeValue first;
    private final AttributeValue second;

    ConditionalWithThreeArguments(String template, String property, AttributeValue first, AttributeValue second) {
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
            .putExpressionValue(firstValueToken, first)
            .putExpressionValue(secondValueToken, second)
            .build();
    }

}
