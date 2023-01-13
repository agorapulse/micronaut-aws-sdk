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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class InListConditional implements QueryConditional {

    private final String property;
    private final List<AttributeValue> values;

    InListConditional(String property, List<AttributeValue> values) {
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
            builder.putExpressionValue(valueName, values.get(i));
        }

        builder.expression(propertyKeyToken + " IN  (" + String.join(", ", valueNames) + ")");

        return builder.build();
    }

}
