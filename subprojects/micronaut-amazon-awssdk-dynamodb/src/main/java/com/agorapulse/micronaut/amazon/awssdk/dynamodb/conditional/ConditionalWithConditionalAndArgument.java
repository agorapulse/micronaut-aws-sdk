/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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

class ConditionalWithConditionalAndArgument implements QueryConditional {

    private final String template;
    private final QueryConditional first;
    private final AttributeValue value;

    ConditionalWithConditionalAndArgument(String template, QueryConditional first, AttributeValue value) {
        this.template = template;
        this.first = first;
        this.value = value;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        Expression ex = first.expression(tableSchema, indexName);
        String propertyValueToken = QueryConditionalFactory.expressionValue("VAL");

        Expression.Builder result = Expression.builder().expression(String.format(template, ex.expression(), propertyValueToken));

        if (ex.expressionNames() != null) {
            ex.expressionNames().forEach(result::putExpressionName);
        }

        if (ex.expressionValues() != null) {
            ex.expressionValues().forEach(result::putExpressionValue);
        }

        result.putExpressionValue(propertyValueToken, value);

        return result.build();
    }

}
