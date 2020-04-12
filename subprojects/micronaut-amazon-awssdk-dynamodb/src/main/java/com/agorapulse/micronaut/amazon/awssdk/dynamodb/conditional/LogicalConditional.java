/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
        builder.expression(expressions.stream().map(Expression::expression).filter(e -> e != null && !e.isEmpty()).collect(joining(token)));
        expressions.forEach(ex -> {
            if (ex.expressionNames() != null) {
                ex.expressionNames().forEach(builder::putExpressionName);
            }
            if (ex.expressionValues() != null) {
                ex.expressionValues().forEach(builder::putExpressionValue);
            }
        });
        return builder.build();
    }

}
