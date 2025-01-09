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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.events.DynamoDbEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class DefaultUpdateBuilder<T, R> implements UpdateBuilder<T, R> {

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
    public UpdateBuilder<T, R> partitionKey(Object key) {
        this.__hash = key;
        return this;
    }

    @Override
    public UpdateBuilder<T, R> sortKey(Object key) {
        this.__range = key;
        return this;
    }

    @Override
    public UpdateBuilder<T, R> add(String attributeName, Object delta) {
        __updates.add(new Update(attributeName, AttributeAction.ADD, delta));
        return this;
    }

    @Override
    public UpdateBuilder<T, R> put(String attributeName, Object value) {
        __updates.add(new Update(attributeName, AttributeAction.PUT, value));
        return this;
    }

    @Override
    public UpdateBuilder<T, R> delete(String attributeName) {
        __updates.add(new Update(attributeName, AttributeAction.DELETE, null));
        return this;
    }

    @Override
    public UpdateBuilder<T, R> configure(Consumer<UpdateItemRequest.Builder> configurer) {
        this.__configurer = configurer;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <N> UpdateBuilder<T, N> returns(ReturnValue returnValue, Function<T, N> mapper) {
        this.__returnValue = returnValue;
        this.__returnValueMapper = mapper;

        return (UpdateBuilder<T, N>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R update(DynamoDbTable<T> mapper, DynamoDbClient client, AttributeConversionHelper attributeConversionHelper, ApplicationEventPublisher publisher) {
        UpdateItemRequest request = resolveRequest(mapper, attributeConversionHelper);
        T keyItem = mapper.tableSchema().mapToItem(request.key());
        publisher.publishEvent(DynamoDbEvent.preUpdate(keyItem));

        UpdateItemResponse result = client.updateItem(request);
        Map<String, AttributeValue> attributes = result.attributes();

        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        if (ReturnValue.NONE.equals(__returnValue)) {
            return null;
        }

        T item = mapper.tableSchema().mapToItem(attributes);
        publisher.publishEvent(DynamoDbEvent.postUpdate(item));
        return (R) __returnValueMapper.apply(item);
    }

    @Override
    public Publisher<R> update(DynamoDbAsyncTable<T> mapper, DynamoDbAsyncClient client, AttributeConversionHelper attributeConversionHelper, ApplicationEventPublisher publisher) {
        UpdateItemRequest request = resolveRequest(mapper, attributeConversionHelper);
        T keyItem = mapper.tableSchema().mapToItem(request.key());
        publisher.publishEvent(DynamoDbEvent.preUpdate(keyItem));

        return Mono.fromFuture(client.updateItem(request))
            .flatMap(result -> {
                if (ReturnValue.NONE.equals(__returnValue)) {
                    return Mono.just((R) keyItem);
                }

                Map<String, AttributeValue> attributes = result.attributes();

                if (attributes == null || attributes.isEmpty()) {
                    return Mono.just((R) keyItem);
                }

                T item = mapper.tableSchema().mapToItem(attributes);
                publisher.publishEvent(DynamoDbEvent.postUpdate(item));
                return Mono.just((R)__returnValueMapper.apply(item));
            });
    }

    @Override
    public UpdateItemRequest resolveRequest(MappedTableResource<T> mapper, AttributeConversionHelper attributeConversionHelper) {
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
