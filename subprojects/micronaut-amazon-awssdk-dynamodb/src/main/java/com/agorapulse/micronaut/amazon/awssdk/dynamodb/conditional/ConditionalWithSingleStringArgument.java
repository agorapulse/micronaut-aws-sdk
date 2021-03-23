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
