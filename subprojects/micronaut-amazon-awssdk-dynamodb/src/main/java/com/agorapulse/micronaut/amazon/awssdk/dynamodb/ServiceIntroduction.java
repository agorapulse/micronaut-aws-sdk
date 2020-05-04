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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.HashKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.RangeKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Scan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.*;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Introduction for {@link com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service} annotation.
 */
@Singleton
public class ServiceIntroduction implements MethodInterceptor<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceIntroduction.class);

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

        AttributeValue getPartitionAttributeValue(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, AttributeConversionHelper attributeConversionHelper) {
            Object hashKeyRaw = params.get(partitionKey.getName()).getValue();
            String hashKeyName = table.tableSchema().tableMetadata().primaryPartitionKey();
            return attributeConversionHelper.convert(table, hashKeyName, hashKeyRaw);
        }

        AttributeValue getSortAttributeValue(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, AttributeConversionHelper attributeConversionHelper) {
            Object rangeKeyRaw = params.get(sortKey.getName()).getValue();
            String rangeKeyName = table.tableSchema().tableMetadata().primarySortKey().orElseThrow(() -> new IllegalArgumentException("Sort key not present for " + table.tableSchema().itemType()));
            return attributeConversionHelper.convert(table, rangeKeyName, rangeKeyRaw);
        }

        Flowable<AttributeValue> getSortAttributeValues(Map<String, MutableArgumentValue<?>> params, DynamoDbTable<?> table, AttributeConversionHelper attributeConversionHelper) {
            final String key = table.tableSchema().tableMetadata().primarySortKey().orElseThrow(() -> new IllegalArgumentException("Sort key not present for " + table.tableSchema().itemType()));
            return toFlowable(Object.class, sortKey, params).map(o -> attributeConversionHelper.convert(table, key, o));
        }

    }

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient client;
    private final AttributeConversionHelper attributeConversionHelper;
    private final FunctionEvaluator functionEvaluator;

    public ServiceIntroduction(
        DynamoDbEnhancedClient enhancedClient,
        DynamoDbClient client,
        AttributeConversionHelper attributeConversionHelper,
        FunctionEvaluator functionEvaluator
    ) {
        this.enhancedClient = enhancedClient;
        this.client = client;
        this.attributeConversionHelper = attributeConversionHelper;
        this.functionEvaluator = functionEvaluator;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<Service> serviceAnnotationValue = context.getAnnotation(Service.class);

        if (serviceAnnotationValue == null) {
            throw new IllegalStateException("Invocation context is missing required annotation Service");
        }

        return doIntercept(context, serviceAnnotationValue);
    }

    private static <T> Flowable<T> toFlowable(Class<T> type, Argument<?> itemArgument, Map<String, MutableArgumentValue<?>> params) {
        Object item = params.get(itemArgument.getName()).getValue();

        if (itemArgument.getType().isArray() && type.isAssignableFrom(itemArgument.getType().getComponentType())) {
            return Flowable.fromArray((T[])item);
        }

        if (Iterable.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return Flowable.fromIterable((Iterable<T>) item);
        }

        if (Flowable.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return Flowable.fromIterable((Iterable<T>) item);
        }

        return Flowable.just((T) item);
    }

    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, AnnotationValue<Service> serviceAnnotationValue) {
        Class<T> type = (Class<T>) serviceAnnotationValue.classValue().orElseThrow(() -> new IllegalArgumentException("Annotation is missing the type value!"));
        String tableName = serviceAnnotationValue.stringValue("tableName").orElseGet(type::getSimpleName);
        // TODO: bean table schema using introspection
        DynamoDbTable<T> table = enhancedClient.table(tableName, BeanTableSchema.create(type));

        try {
            return doIntercept(context, type, table);
        } catch (ResourceNotFoundException ignored) {
            createTable(table);
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
            DetachedQuery<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Query.class).value(), context);

            if (methodName.startsWith("count")) {
                return criteria.count(table, attributeConversionHelper);
            }

            Flowable<T> queryResult = criteria.query(table, attributeConversionHelper);
            if (methodName.startsWith("delete")) {
                return deleteAll(table, queryResult);
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T> update = (UpdateBuilder<T>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return updateAll(table, queryResult, update);
            }

            return flowableOrList(queryResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Scan.class)) {
            DetachedScan<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Scan.class).value(), context);

            if (methodName.startsWith("count")) {
                return criteria.count(table, attributeConversionHelper);
            }

            Flowable<T> scanResult = criteria.scan(table, attributeConversionHelper);

            if (methodName.startsWith("delete")) {
                return deleteAll(table, scanResult);
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T> update = (UpdateBuilder<T>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return updateAll(table, scanResult, update);
            }

            return flowableOrList(scanResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
            DetachedUpdate<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);

            return criteria.update(table, client, attributeConversionHelper);
        }

        if (methodName.startsWith("count")) {
            return simpleHashAndRangeQuery(type, context, table).count(table, attributeConversionHelper);
        }

        if (methodName.startsWith("delete")) {
            return handleDelete(type, table, context);
        }

        if (methodName.startsWith("query") || methodName.startsWith("findAll") || methodName.startsWith("list")) {
            return flowableOrList(simpleHashAndRangeQuery(type, context, table).query(table, attributeConversionHelper), context.getReturnType().getType());
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod());
    }

    private <T> int updateAll(DynamoDbTable<T> table, Flowable<T> queryResult, UpdateBuilder<T> update) {
        // there is no batch update API, we can do batch updates in transaction but in that case it would cause
        // doubling the writes

        BeanIntrospection<T> introspection = EntityIntrospection.getBeanIntrospection(table);
        TableMetadata tableMetadata = table.tableSchema().tableMetadata();

        AtomicInteger counter = new AtomicInteger();

        queryResult.subscribe(entity -> {
            introspection.getProperty(tableMetadata.primaryPartitionKey()).ifPresent(p -> update.partitionKey(p.get(entity)));
            tableMetadata.primarySortKey().flatMap(introspection::getProperty).ifPresent(p -> update.sortKey(p.get(entity)));

            update.update(table, client, attributeConversionHelper);

            counter.incrementAndGet();
        });

        return counter.get();
    }

    private Object flowableOrList(Flowable<?> result, Class<?> type) {
        if (List.class.isAssignableFrom(type)) {
            return result.toList().blockingGet();
        }
        return result;
    }

    private <T> Object handleSave(DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 1) {
            throw new UnsupportedOperationException("Method expects at most 1 parameters - item, iterable of items or array of items");
        }

        Argument<?> itemArgument = args[0];
        Flowable<T> items = toFlowable(service.tableSchema().itemType().rawClass(), itemArgument, params);

        if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Flowable.class.isAssignableFrom(itemArgument.getType())) {
            return flowableOrList(saveAll(service, items), context.getReturnType().getType());
        }

        return service.updateItem(items.blockingFirst());
    }

    private <T> Object handleDelete(Class<T> type, DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length == 1) {
            Argument<?> itemArgument = args[0];
            Flowable<T> items = toFlowable(service.tableSchema().itemType().rawClass(), itemArgument, params);

            if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Flowable.class.isAssignableFrom(itemArgument.getType())) {
                return deleteAll(service, items);
            }

            if (type.isAssignableFrom(itemArgument.getType())) {
                service.deleteItem(service.keyFrom(items.blockingFirst()));
                return items.blockingFirst();
            }
        }

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - hash key and range key, an item, iterable of items or an array of items");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args, service);

        if (partitionAndSort.sortKey == null) {
            service.deleteItem(Key.builder().partitionValue(partitionAndSort.getPartitionAttributeValue(params, service, attributeConversionHelper)).build());
            return 1;
        }

        service.deleteItem(
            Key.builder()
                .partitionValue(partitionAndSort.getPartitionAttributeValue(params, service, attributeConversionHelper))
                .sortValue(partitionAndSort.getSortAttributeValue(params, service, attributeConversionHelper))
                .build()
        );

        return 1;
    }

    private <T> Object handleGet(DynamoDbTable<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - hash key and range key");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args, service);
        AttributeValue partitionValue = partitionAndSort.getPartitionAttributeValue(params, service, attributeConversionHelper);

        if (partitionAndSort.sortKey == null) {
            return service.getItem(Key.builder().partitionValue(partitionValue).build());
        }

        if (
            partitionAndSort.sortKey.getType().isArray()
                || Iterable.class.isAssignableFrom(partitionAndSort.sortKey.getType())
                || Flowable.class.isAssignableFrom(partitionAndSort.sortKey.getType())
        ) {
            return flowableOrList(getAll(service, partitionValue, partitionAndSort.getSortAttributeValues(params, service, attributeConversionHelper)), context.getReturnType().getType());
        }

        return service.getItem(Key.builder().partitionValue(partitionValue).sortValue(partitionAndSort.getSortAttributeValue(params, service, attributeConversionHelper)).build());
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

        PartitionAndSort partitionAndSort = findHashAndRange(args, table);

        AttributeValue partitionAttributeValue = partitionAndSort.getPartitionAttributeValue(params, table, attributeConversionHelper);

        if (partitionAndSort.sortKey == null) {
            return Builders.query(type, q -> {
                q.partitionKey(partitionAttributeValue);
            });
        }

        AttributeValue sortAttributeValue = partitionAndSort.getSortAttributeValue(params, table, attributeConversionHelper);

        return Builders.query(q -> q.partitionKey(partitionAttributeValue).sortKey(r -> r.eq(sortAttributeValue)));
    }

    private PartitionAndSort findHashAndRange(Argument<?>[] arguments, DynamoDbTable<?> table) {
        PartitionAndSort names = new PartitionAndSort();
        for (Argument<?> argument : arguments) {
            if (
                argument.isAnnotationPresent(SortKey.class)
                    || argument.isAnnotationPresent(RangeKey.class)
                    || argument.getName().toLowerCase().contains(SORT)
                    || argument.getName().toLowerCase().contains(RANGE)
                    || argument.getName().equals(table.tableSchema().tableMetadata().primarySortKey().orElse(SORT))
            ) {
                names.sortKey = argument;
            } else if (
                argument.isAnnotationPresent(PartitionKey.class)
                    || argument.isAnnotationPresent(HashKey.class)
                    || argument.getName().toLowerCase().contains(PARTITION)
                    || argument.getName().toLowerCase().contains(HASH)
                    || argument.getName().equals(table.tableSchema().tableMetadata().primaryPartitionKey())
            ) {
                names.partitionKey = argument;
            }
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument annotated with @HashKey or with called 'hash'");
        }

        return names;
    }

    private <T> Flowable<T> saveAll(DynamoDbTable<T> service, Flowable<T> itemsToSave) {
        List<T> unprocessed = itemsToSave.buffer(BATCH_SIZE).map(batchItems -> enhancedClient.batchWriteItem(b -> {
            b.writeBatches(batchItems.stream().map(i ->
                WriteBatch.builder(service.tableSchema().itemType().rawClass()).mappedTableResource(service).addPutItem(i).build()
            ).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.unprocessedPutItemsForTable(service))).toList().blockingGet();

        if (unprocessed.isEmpty()) {
            return itemsToSave;
        }

        throw new IllegalArgumentException("Following items couldn't be saved:" + unprocessed.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    private <T> int deleteAll(DynamoDbTable<T> table, Flowable<T> items) {
        TableSchema<T> tableSchema = table.tableSchema();
        AtomicInteger counter = new AtomicInteger();
        List<Key> unprocessed = items.buffer(BATCH_SIZE).map(batchItems -> enhancedClient.batchWriteItem(b -> {
            counter.addAndGet(batchItems.size());
            b.writeBatches(batchItems.stream().map(i ->
                WriteBatch.builder(tableSchema.itemType().rawClass()).mappedTableResource(table).addDeleteItem(i).build()
            ).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.unprocessedDeleteItemsForTable(table))).toList().blockingGet();

        if (unprocessed.isEmpty()) {
            return counter.get();
        }

        throw new IllegalArgumentException("Following items couldn't be deleted:" + unprocessed.stream()
            .map(k -> tableSchema.mapToItem(k.keyMap(tableSchema, TableMetadata.primaryIndexName()))).map(Object::toString)
            .collect(Collectors.joining(", ")));
    }

    private <T> Flowable<T> getAll(DynamoDbTable<T> service, AttributeValue hashKey, Flowable<AttributeValue> rangeKeys) {
        TableSchema<T> tableSchema = service.tableSchema();
        return rangeKeys.buffer(BATCH_SIZE).map(batchRangeKeys -> enhancedClient.batchGetItem(b -> {
            b.readBatches(batchRangeKeys.stream().map(k ->
                ReadBatch.builder(tableSchema.itemType().rawClass()).mappedTableResource(service).addGetItem(Key.builder().partitionValue(hashKey).sortValue(k).build()).build()
            ).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.resultsForTable(service)));
    }

    private <T> void createTable(DynamoDbTable<T> table) {
        Map<String, ProjectionType> types = getProjectionTypes(table);
        TableMetadata tableMetadata = table.tableSchema().tableMetadata();
        tableMetadata.allKeys();
        table.createTable(b -> {
            List<EnhancedLocalSecondaryIndex> localSecondaryIndices = new ArrayList<>();
            List<EnhancedGlobalSecondaryIndex> globalSecondaryIndices = new ArrayList<>();

            getIndices(tableMetadata).forEach(i -> {
                if (TableMetadata.primaryIndexName().equals(i)) {
                    return;
                }
                ProjectionType type = types.getOrDefault(i, ProjectionType.KEYS_ONLY);
                if (tableMetadata.primaryPartitionKey().equals(tableMetadata.indexPartitionKey(i))) {
                    localSecondaryIndices.add(EnhancedLocalSecondaryIndex.create(i, Projection.builder().projectionType(type).build()));
                } else {
                    globalSecondaryIndices.add(EnhancedGlobalSecondaryIndex.builder().indexName(i).projection(Projection.builder().projectionType(type).build()).build());
                }
            });

            if (!localSecondaryIndices.isEmpty()) {
                b.localSecondaryIndices(localSecondaryIndices);
            }

            if (!globalSecondaryIndices.isEmpty()) {
                b.globalSecondaryIndices(globalSecondaryIndices);
            }
        });

    }

    private <T> Map<String, ProjectionType> getProjectionTypes(DynamoDbTable<T> table) {
        Map<String, ProjectionType> types = new HashMap<>();

        BeanIntrospection<T> introspection = EntityIntrospection.getBeanIntrospection(table);
        introspection.getBeanProperties().forEach(p -> {
            AnnotationValue<com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection> projectionAnnotation = p.getAnnotation(com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection.class);

            if (projectionAnnotation == null) {
                return;
            }

            AnnotationValue<DynamoDbSecondarySortKey> secondaryIndex = p.getAnnotation(DynamoDbSecondarySortKey.class);
            Collection<String> indexNames = new ArrayList<>();
            if (secondaryIndex != null) {
                indexNames.addAll(Arrays.asList(secondaryIndex.stringValues("indexNames")));
            }

            AnnotationValue<DynamoDbSecondaryPartitionKey> secondaryGlobalIndex = p.getAnnotation(DynamoDbSecondaryPartitionKey.class);
            if (secondaryGlobalIndex != null) {
                indexNames.addAll(Arrays.asList(secondaryGlobalIndex.stringValues("indexNames")));
            }

            if (indexNames.isEmpty()) {
                return;
            }

            ProjectionType type = projectionAnnotation.enumValue(ProjectionType.class).orElse(ProjectionType.KEYS_ONLY);
            for(String name : indexNames) {
                types.put(name, type);
            }
        });

        return types;
    }

    private Set<String> getIndices(TableMetadata metadata) {
        if (metadata instanceof StaticTableMetadata) {
            try {
                Field indexByNameMapField = StaticTableMetadata.class.getDeclaredField("indexByNameMap");
                indexByNameMapField.setAccessible(true);
                Map<String, ?> indexByNameMap = (Map<String, ?>) indexByNameMapField.get(metadata);
                return indexByNameMap.keySet();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.error("Exception reading indices", e);
            }
        }

        return Collections.emptySet();
    }
}
