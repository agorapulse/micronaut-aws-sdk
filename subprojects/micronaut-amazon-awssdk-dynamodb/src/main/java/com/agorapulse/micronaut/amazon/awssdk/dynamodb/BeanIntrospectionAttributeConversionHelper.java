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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class BeanIntrospectionAttributeConversionHelper implements AttributeConversionHelper {

    @Override
    public <T> Map<String, AttributeValue> convert(DynamoDbTable<T> table, Map<String, Object> values) {
        BeanIntrospection<T> introspection = getBeanIntrospection(table);
        T instance = introspection.instantiate();
        return values.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> convert(introspection, table, instance, e.getKey(), e.getValue())
        ));
    }

    private <T> BeanIntrospection<T> getBeanIntrospection(DynamoDbTable<T> table) {
        return BeanIntrospector.SHARED.findIntrospection(table.tableSchema().itemType().rawClass())
            .orElseThrow(() -> new IllegalArgumentException("No introspection found for " + table.tableSchema().itemType().rawClass()
                + "! Please, see https://docs.micronaut.io/latest/guide/index.html#introspection for more details")
            );
    }

    private <T> AttributeValue convert(BeanIntrospection<T> introspection, DynamoDbTable<T> table, T instance, String key, Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof AttributeValue) {
            return (AttributeValue) value;
        }

        BeanProperty<T, Object> p = introspection.getProperty(key).orElseThrow(() -> new IllegalArgumentException("Unknown property " + key + " for " + instance));
        p.set(instance, value);
        return table.tableSchema().attributeValue(instance, key);
    }

}
