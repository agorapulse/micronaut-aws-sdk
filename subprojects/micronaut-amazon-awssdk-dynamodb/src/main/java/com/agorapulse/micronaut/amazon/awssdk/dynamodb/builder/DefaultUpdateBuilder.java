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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeValueConverter;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.function.BiConsumer;
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

    private final List<Update> updates = new ArrayList<>();
    private final List<BiConsumer<Key.Builder, DynamoDbTable<T>>> keyDefinitions = new ArrayList<>();

    private ReturnValue returnValue = ReturnValue.NONE;
    private Function<T, ?> returnValueMapper = Function.identity();

    private Consumer<UpdateItemRequest.Builder> configurer = u -> {};

    @Override
    public UpdateBuilder<T> key(BiConsumer<Key.Builder, DynamoDbTable<T>> definition) {
        keyDefinitions.add(definition);
        return this;
    }

    @Override
    public UpdateBuilder<T> add(String attributeName, Object delta) {
        updates.add(new Update(attributeName, AttributeAction.ADD, delta));
        return this;
    }

    @Override
    public UpdateBuilder<T> put(String attributeName, Object value) {
        updates.add(new Update(attributeName, AttributeAction.PUT, value));
        return this;
    }

    @Override
    public UpdateBuilder<T> delete(String attributeName) {
        updates.add(new Update(attributeName, AttributeAction.DELETE, null));
        return this;
    }

    @Override
    public UpdateBuilder<T> configure(Consumer<UpdateItemRequest.Builder> configurer) {
        this.configurer = configurer;
        return this;
    }

    @Override
    public UpdateBuilder<T> returns(ReturnValue returnValue, Function<T, ?> mapper) {
        this.returnValue = returnValue;
        this.returnValueMapper = mapper;

        return this;
    }

    @Override
    public Object update(DynamoDbTable<T> mapper, DynamoDbClient client, AttributeValueConverter converter) {
        UpdateItemRequest request = resolveExpression(mapper, converter);
        UpdateItemResponse result = client.updateItem(request);
        Map<String, AttributeValue> attributes = result.attributes();

        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        if (ReturnValue.NONE.equals(returnValue)) {
            return null;
        }

        return returnValueMapper.apply(mapper.tableSchema().mapToItem(attributes));
    }

    @Override
    public UpdateItemRequest resolveExpression(DynamoDbTable<T> mapper, AttributeValueConverter converter) {
        UpdateItemRequest.Builder builder = UpdateItemRequest.builder();
        configurer.accept(builder);

        builder.tableName(mapper.tableName());

        Key.Builder key = Key.builder();
        keyDefinitions.forEach(b -> b.accept(key, mapper));
        builder.key(key.build().primaryKeyMap(mapper.tableSchema()));

        builder.returnValues(returnValue);


        // TODO: switch to update expressions
        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();

        Map<String, AttributeValue> converted = converter.convert(mapper, updates.stream().collect(Collectors.toMap(
            u -> u.name,
            u -> u.value
        )));

        for (Update u : updates) {
            attributeUpdates.put(
                u.name,
                AttributeValueUpdate.builder().action(u.action).value(converted.get(u.name)).build()
            );
        }

        builder.attributeUpdates(attributeUpdates);

        configurer.accept(builder);

        return builder.build();
    }

}
