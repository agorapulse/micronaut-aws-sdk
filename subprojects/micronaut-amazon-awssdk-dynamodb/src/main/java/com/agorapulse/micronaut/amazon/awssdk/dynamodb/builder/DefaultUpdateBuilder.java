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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class DefaultUpdateBuilder<T> implements UpdateBuilder<T> {

    private static class Update {
        final String name;
        final AttributeAction action;
        final Object value;

        private Update(String name, AttributeAction action, Object value) {
            this.name = name;
            this.action = action;
            this.value = value;
        }
    }

    // fields are prefixed with "__" to allow groovy evaluation of the arguments
    // otherwise if the argument has the same name (such as max) it will be ignored and field value will be used
    private final List<Update> __updates = new ArrayList<>();
    private ReturnValue __returnValue = ReturnValue.NONE;
    private Function<T, ?> __returnValueMapper = Function.identity();
    private Object __hash;
    private Object __range;

    private Consumer<UpdateItemRequest.Builder> __configurer = u -> {};

    @Override
    public UpdateBuilder<T> hash(Object key) {
        this.__hash = key;
        return this;
    }

    @Override
    public UpdateBuilder<T> range(Object key) {
        this.__range = key;
        return this;
    }

    @Override
    public UpdateBuilder<T> add(String attributeName, Object delta) {
        __updates.add(new Update(attributeName, AttributeAction.ADD, delta));
        return this;
    }

    @Override
    public UpdateBuilder<T> put(String attributeName, Object value) {
        __updates.add(new Update(attributeName, AttributeAction.PUT, value));
        return this;
    }

    @Override
    public UpdateBuilder<T> delete(String attributeName) {
        __updates.add(new Update(attributeName, AttributeAction.DELETE, null));
        return this;
    }

    @Override
    public UpdateBuilder<T> configure(Consumer<UpdateItemRequest.Builder> configurer) {
        this.__configurer = configurer;
        return this;
    }

    @Override
    public UpdateBuilder<T> returns(ReturnValue returnValue, Function<T, ?> mapper) {
        this.__returnValue = returnValue;
        this.__returnValueMapper = mapper;

        return this;
    }

    @Override
    public Object update(DynamoDbTable<T> mapper, DynamoDbClient client, AttributeConversionHelper attributeConversionHelper) {
        UpdateItemRequest request = resolveExpression(mapper, attributeConversionHelper);
        UpdateItemResponse result = client.updateItem(request);
        Map<String, AttributeValue> attributes = result.attributes();

        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        if (ReturnValue.NONE.equals(__returnValue)) {
            return null;
        }

        return __returnValueMapper.apply(mapper.tableSchema().mapToItem(attributes));
    }

    @Override
    public UpdateItemRequest resolveExpression(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        UpdateItemRequest.Builder builder = UpdateItemRequest.builder();
        __configurer.accept(builder);

        builder.tableName(mapper.tableName());

        Key.Builder key = Key.builder();

        if (__range != null) {
            String sortKey = mapper.tableSchema().tableMetadata().primarySortKey().orElseThrow(() -> new IllegalArgumentException("Range key defined for update but none present on entity " + mapper.tableSchema().itemType().rawClass()));
            key.sortValue(attributeConversionHelper.convert(mapper, sortKey, __range));
        }

        if (__hash != null) {
            String partitionKey = mapper.tableSchema().tableMetadata().primaryPartitionKey();
            key.partitionValue(attributeConversionHelper.convert(mapper, partitionKey, __hash));
        }

        builder.key(key.build().primaryKeyMap(mapper.tableSchema()));
        builder.returnValues(__returnValue);

        // TODO: switch to update expressions
        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();

        Map<String, AttributeValue> converted = attributeConversionHelper.convert(
            mapper,
            __updates.stream()
                .filter(u -> u.value != null)
                .collect(Collectors.toMap(
                    u -> u.name,
                    u -> u.value
                ))
        );

        for (Update u : __updates) {
            attributeUpdates.put(
                u.name,
                AttributeValueUpdate.builder().action(u.action).value(converted.get(u.name)).build()
            );
        }

        builder.attributeUpdates(attributeUpdates);

        __configurer.accept(builder);

        return builder.build();
    }

}
