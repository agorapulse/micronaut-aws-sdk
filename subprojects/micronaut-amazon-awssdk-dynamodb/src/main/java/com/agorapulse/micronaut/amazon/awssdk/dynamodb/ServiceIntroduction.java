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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.StrictMap;
import groovy.lang.Closure;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import io.reactivex.Flowable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Introduction for {@link com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service} annotation.
 */
@Singleton
public class ServiceIntroduction implements MethodInterceptor<Object, Object> {

    private static final String PARTITION = "partition";
    private static final String SORT = "sort";
    private static final String HASH = "hash";
    private static final String RANGE = "range";
    private static final int BATCH_SIZE = 25;

    private static class PartitionAndSort {
        Argument<?> partitionKey;
        Argument<?> sortKey;

        boolean isValid() {
            return partitionKey != null;
        }

        AttributeValue getPartitionAttributeValue(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, Converter converter) {
            Object hashKeyRaw = params.get(partitionKey.getName()).getValue();
            String hashKeyName = table.tableSchema().tableMetadata().primaryPartitionKey();
            return converter.convert(table, hashKeyName, hashKeyRaw);
        }

        AttributeValue getSortAttributeValue(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, Converter converter) {
            Object rangeKeyRaw = params.get(sortKey.getName()).getValue();
            String rangeKeyName = table.tableSchema().tableMetadata().primarySortKey().orElseThrow(() -> new IllegalArgumentException("Sort key not present for " + table.tableSchema().itemType()));
            return converter.convert(table, rangeKeyName, rangeKeyRaw);
        }

        List<AttributeValue> getSortAttributeValues(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, Converter converter) {
            final String key = table.tableSchema().tableMetadata().primarySortKey().orElseThrow(() -> new IllegalArgumentException("Sort key not present for " + table.tableSchema().itemType()));
            return toList(Object.class, sortKey, params).stream().map(o -> converter.convert(table, key, o)).collect(Collectors.toList());
        }

    }

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient client;
    private final Converter converter;

    public ServiceIntroduction(DynamoDbEnhancedClient enhancedClient, DynamoDbClient client, Converter converter) {
        this.enhancedClient = enhancedClient;
        this.client = client;
        this.converter = converter;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<Service> serviceAnnotationValue = context.getAnnotation(Service.class);

        if (serviceAnnotationValue == null) {
            throw new IllegalStateException("Invocation context is missing required annotation Service");
        }

        return doIntercept(context, serviceAnnotationValue);
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable instanceof List) {
            return (List<T>) iterable;
        }

        List<T> ret = new ArrayList<>();
        iterable.forEach(ret::add);
        return ret;
    }

    private static <T> List<List<T>> partition(List<T> collection, int chunkSize) {
        List<List<T>> lists = new ArrayList<>();
        for (int i = 0; i < collection.size(); i += chunkSize) {
            int end = Math.min(collection.size(), i + chunkSize);
            lists.add(collection.subList(i, end));
        }
        return lists;
    }

    private static <T> List<T> toList(Class<T> type, Argument<?> itemArgument, Map<String, MutableArgumentValue<?>> params) {
        Object item = params.get(itemArgument.getName()).getValue();

        if (itemArgument.getType().isArray() && type.isAssignableFrom(itemArgument.getType().getComponentType())) {
            return Arrays.asList((T[])item);
        }

        if (Iterable.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return toList((Iterable<T>) item);
        }

        if (type.isAssignableFrom(itemArgument.getType())) {
            return Collections.singletonList((T) item);
        }
        throw new IllegalArgumentException("Argument " + itemArgument + " cannot be cast to list of " + type);
    }

    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, AnnotationValue<Service> serviceAnnotationValue) {
        Class<T> type = (Class<T>) serviceAnnotationValue.classValue().orElseThrow(() -> new IllegalArgumentException("Annotation is missing the type value!"));
        String tableName = serviceAnnotationValue.stringValue("tableName").orElseGet(type::getSimpleName);
        // TODO: bean table schema using introspection
        DynamoDbTable<T> table = enhancedClient.table(tableName, BeanTableSchema.create(type));

        try {
            return doIntercept(context, type, table);
        } catch (ResourceNotFoundException ignored) {
            table.createTable();
            return doIntercept(context, type, table);
        }
    }

    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, Class<T> type, DynamoDbTable<T> table) {
        String methodName = context.getMethodName();
        if (methodName.startsWith("save")) {
            return handleSave(table, context);
        }

        if (methodName.startsWith("get") || methodName.startsWith("load")) {
            return handleGet(table, context);
        }

        if (context.getTargetMethod().isAnnotationPresent(Query.class)) {
            DetachedQuery<T> criteria = evaluateAnnotationType(context.getTargetMethod().getAnnotation(Query.class).value(), context);

            if (methodName.startsWith("count")) {
                return criteria.count(table, converter);
            }

            if (methodName.startsWith("delete")) {
                int counter = 0;
                for (T t : table.query(criteria.resolveRequest(table, converter)).items()) {
                    table.deleteItem(t);
                    counter++;
                }
                return counter;
            }

            return flowableOrList(criteria.query(table, converter), context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
            DetachedUpdate<T> criteria = evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);

            return criteria.update(table, client, converter);
        }

        if (context.getTargetMethod().isAnnotationPresent(Scan.class)) {
            DetachedScan<T> criteria = evaluateAnnotationType(context.getTargetMethod().getAnnotation(Scan.class).value(), context);

            if (methodName.startsWith("count")) {
                return criteria.count(table, converter);
            }

            if (methodName.startsWith("delete")) {
                int counter = 0;
                for (T t : table.scan(criteria.resolveRequest(table, converter)).items()) {
                    table.deleteItem(t);
                    counter++;
                }
                return counter;
            }

            return flowableOrList(criteria.scan(table, converter), context.getReturnType().getType());
        }

        if (methodName.startsWith("count")) {
            return simpleHashAndRangeQuery(type, context, table).count(table, converter);
        }

        if (methodName.startsWith("delete")) {
            return handleDelete(type, table, context);
        }

        if (methodName.startsWith("query") || methodName.startsWith("findAll") || methodName.startsWith("list")) {
            return flowableOrList(simpleHashAndRangeQuery(type, context, table).query(table, converter), context.getReturnType().getType());
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private Object flowableOrList(Flowable<?> result, Class<?> type) {
        if (List.class.isAssignableFrom(type)) {
            return result.toList().blockingGet();
        }
        return result;
    }

    private <T, F extends Function<Map<String, Object>, T>> T evaluateAnnotationType(Class<F> updateDefinitionType, MethodInvocationContext<Object, Object> context) {
        Map<String, Object> parameterValueMap = new StrictMap<>(context.getParameterValueMap());

        if (Closure.class.isAssignableFrom(updateDefinitionType)) {
            try {
                Closure<T> closure = (Closure<T>) updateDefinitionType.getConstructor(Object.class, Object.class).newInstance(parameterValueMap, parameterValueMap);
                closure.setDelegate(parameterValueMap);
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                return closure.call(parameterValueMap);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot instantiate closure! Type: " + updateDefinitionType, e);
            }
        }

        F function = BeanIntrospector.SHARED.findIntrospection(updateDefinitionType).map(BeanIntrospection::instantiate)
            .orElseGet(() -> {
                try {
                    return updateDefinitionType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Cannot instantiate function! Type: " + updateDefinitionType, e);
                }
            });
        return function.apply(parameterValueMap);
    }

    private <T> Object handleSave(DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 1) {
            throw new UnsupportedOperationException("Method expects at most 1 parameters - item, iterable of items or array of items");
        }

        Argument<?> itemArgument = args[0];
        List<T> items = toList(service.tableSchema().itemType().rawClass(), itemArgument, params);

        if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType())) {
            return saveAll(service, items);
        }

        return service.updateItem(items.get(0));
    }

    private <T> Object handleDelete(Class<T> type, DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length == 1) {
            Argument<?> itemArgument = args[0];
            List<T> items = toList(service.tableSchema().itemType().rawClass(), itemArgument, params);

            if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType())) {
                deleteAll(service, items);
                return null;
            }

            if (type.isAssignableFrom(itemArgument.getType())) {
                service.deleteItem(service.keyFrom(items.get(0)));
                return null;
            }
        }

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - hash key and range key, an item, iterable of items or an array of items");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args);

        if (partitionAndSort.sortKey == null) {
            service.deleteItem(Key.builder().partitionValue(partitionAndSort.getPartitionAttributeValue(params, service, converter)).build());
            return null;
        }

        service.deleteItem(
            Key.builder()
                .partitionValue(partitionAndSort.getPartitionAttributeValue(params, service, converter))
                .sortValue(partitionAndSort.getSortAttributeValue(params, service, converter))
                .build()
        );

        return null;
    }

    private <T> Object handleGet(DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - hash key and range key");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args);
        AttributeValue partitionValue = partitionAndSort.getPartitionAttributeValue(params, service, converter);

        if (partitionAndSort.sortKey == null) {
            return service.getItem(Key.builder().partitionValue(partitionValue).build());
        }

        if (partitionAndSort.sortKey.getType().isArray() || Iterable.class.isAssignableFrom(partitionAndSort.sortKey.getType())) {
            return getAll(service, partitionValue, partitionAndSort.getSortAttributeValues(params, service, converter));
        }

        return service.getItem(Key.builder().partitionValue(partitionValue).sortValue(partitionAndSort.getSortAttributeValue(params, service, converter)).build());
    }

    private <T> DetachedQuery<T> simpleHashAndRangeQuery(
        Class<T> type,
        MethodInvocationContext<Object, Object> context,
        DynamoDbTable<T> table
    ) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - hash key and optional range key");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args);

        AttributeValue partitionAttributeValue = partitionAndSort.getPartitionAttributeValue(params, table, converter);

        if (partitionAndSort.sortKey == null) {
            return Builders.query(type, q -> {
                q.hash(partitionAttributeValue);
            });
        }

        AttributeValue sortAttributeValue = partitionAndSort.getSortAttributeValue(params, table, converter);

        return Builders.query(type, q -> q.hash(partitionAttributeValue).range(r -> r.eq(sortAttributeValue)));
    }

    private PartitionAndSort findHashAndRange(Argument<?>[] arguments) {
        PartitionAndSort names = new PartitionAndSort();
        for (Argument<?> argument : arguments) {
            if (argument.isAnnotationPresent(SortKey.class) || argument.getName().toLowerCase().contains(SORT) || argument.getName().toLowerCase().contains(RANGE)) {
                names.sortKey = argument;
            } else if (argument.isAnnotationPresent(PartitionKey.class) || argument.getName().toLowerCase().contains(PARTITION) || argument.getName().toLowerCase().contains(HASH)) {
                names.partitionKey = argument;
            }
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument annotated with @HashKey or with called 'hash'");
        }

        return names;
    }

    private <T> Object saveAll(DynamoDbTable<T> service, List<T> itemsToSave) {
        List<T> unprocessed = partition(itemsToSave, BATCH_SIZE).stream().map(batchItems -> enhancedClient.batchWriteItem(b -> {
            b.writeBatches(batchItems.stream().map(i -> WriteBatch.builder(service.tableSchema().itemType().rawClass()).addPutItem(i).build()).collect(Collectors.toList()));
        })).flatMap(r -> r.unprocessedPutItemsForTable(service).stream()).collect(Collectors.toList());

        if (unprocessed.isEmpty()) {
            return itemsToSave;
        }

        throw new IllegalArgumentException("Following items couldn't be saved:" + unprocessed.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    private <T> void deleteAll(DynamoDbTable<T> service, List<T> itemsToDelete) {
        TableSchema<T> tableSchema = service.tableSchema();
        List<Key> unprocessed = partition(itemsToDelete, BATCH_SIZE).stream().map(batchItems -> enhancedClient.batchWriteItem(b -> {
            b.writeBatches(batchItems.stream().map(i -> WriteBatch.builder(tableSchema.itemType().rawClass()).addDeleteItem(i).build()).collect(Collectors.toList()));
        })).flatMap(r -> r.unprocessedDeleteItemsForTable(service).stream()).collect(Collectors.toList());

        if (unprocessed.isEmpty()) {
            return;
        }

        throw new IllegalArgumentException("Following items couldn't be deleted:" + unprocessed.stream()
            .map(k -> tableSchema.mapToItem(k.keyMap(tableSchema, TableMetadata.primaryIndexName()))).map(Object::toString)
            .collect(Collectors.joining(", ")));
    }

    private <T> List<T> getAll(DynamoDbTable<T> service, AttributeValue hashKey, List<AttributeValue> rangeKeys) {
        TableSchema<T> tableSchema = service.tableSchema();
        return partition(rangeKeys, BATCH_SIZE).stream().map(batchRangeKeys -> enhancedClient.batchGetItem(b -> {
            b.readBatches(batchRangeKeys.stream().map(k -> ReadBatch.builder(tableSchema.itemType().rawClass()).addGetItem(Key.builder().partitionValue(hashKey).sortValue(k).build()).build()).collect(Collectors.toList()));
        })).flatMap(r -> r.resultsForTable(service).stream()).collect(Collectors.toList());
    }
}
