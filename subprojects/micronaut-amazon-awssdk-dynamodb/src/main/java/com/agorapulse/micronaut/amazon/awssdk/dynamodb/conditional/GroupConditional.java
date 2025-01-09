/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

class GroupConditional implements QueryConditional {

    private final QueryConditional statement;

    GroupConditional(QueryConditional statement) {
        this.statement = statement;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        Expression expression = statement.expression(tableSchema, indexName);
        return Expression.builder()
            .expression(String.format("( %s )", expression.expression()))
            .expressionNames(expression.expressionNames())
            .expressionValues(expression.expressionValues())
            .build();
    }

}
