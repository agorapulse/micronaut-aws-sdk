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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.events.DynamoDbEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultDynamoDbService<T> implements DynamoDbService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDynamoDbService.class);

    private static final int BATCH_SIZE = 25;

    private final Class<T> itemType;
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient client;
    private final AttributeConversionHelper attributeConversionHelper;
    private final ApplicationEventPublisher publisher;
    private final DynamoDbTable<T> table;

    public DefaultDynamoDbService(
        Class<T> itemType,
        DynamoDbEnhancedClient enhancedClient,
        DynamoDbClient client,
        AttributeConversionHelper attributeConversionHelper,
        ApplicationEventPublisher publisher,
        DynamoDbTable<T> table
    ) {
        this.itemType = itemType;
        this.enhancedClient = enhancedClient;
        this.client = client;
        this.attributeConversionHelper = attributeConversionHelper;
        this.publisher = publisher;
        this.table = table;
    }

    @Override
    public Class<T> getItemType() {
        return itemType;
    }

    @Override
    public DynamoDbTable<T> getTable() {
        return table;
    }

    @Override
    public Flowable<T> query(DetachedQuery<T> query) {
        return query.query(table, attributeConversionHelper).map(this::postLoad);
    }

    @Override
    public Flowable<T> scan(DetachedScan<T> scan) {
        return scan.scan(table, attributeConversionHelper).map(this::postLoad);
    }

    @Override
    public Flowable<T> findAll(Object partitionKey, Object sortKey) {
        return simplePartitionAndSort(partitionKey, sortKey).query(table, attributeConversionHelper).map(this::postLoad);
    }

    @Override
    public <R> R update(DetachedUpdate<T, R> update) {
        return update.update(table, client, attributeConversionHelper, publisher);
    }

    @Override
    public int updateAll(Flowable<T> items, UpdateBuilder<T, ?> update) {
        // there is no batch update API, we can do batch updates in transaction but in that case it would cause
        // doubling the writes

        BeanIntrospection<T> introspection = EntityIntrospection.getBeanIntrospection(table);
        TableMetadata tableMetadata = table.tableSchema().tableMetadata();

        AtomicInteger counter = new AtomicInteger();

        items.map(this::postLoad).subscribe(entity -> {
            introspection.getProperty(tableMetadata.primaryPartitionKey()).ifPresent(p -> update.partitionKey(p.get(entity)));
            tableMetadata.primarySortKey().flatMap(introspection::getProperty).ifPresent(p -> update.sortKey(p.get(entity)));

            update.update(table, client, attributeConversionHelper, publisher);

            counter.incrementAndGet();
        });

        return counter.get();
    }

    @Override
    public T save(T entity) {
        publisher.publishEvent(DynamoDbEvent.prePersist(entity));
        T updated = table.updateItem(entity);
        publisher.publishEvent(DynamoDbEvent.postPersist(updated));
        return updated;
    }

    @Override
    public Flowable<T> saveAll(Flowable<T> itemsToSave) {
        List<T> saved = new ArrayList<>();
        List<T> unprocessed = itemsToSave.buffer(BATCH_SIZE).map(batchItems -> enhancedClient.batchWriteItem(b -> {
            b.writeBatches(batchItems.stream().map(i -> {
                publisher.publishEvent(DynamoDbEvent.prePersist(i));
                saved.add(i);
                return WriteBatch.builder(table.tableSchema().itemType().rawClass()).mappedTableResource(table).addPutItem(i).build();
            }).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.unprocessedPutItemsForTable(table))).toList().blockingGet();

        if (unprocessed.isEmpty()) {
            saved.forEach(i -> publisher.publishEvent(DynamoDbEvent.postPersist(i)));
            return Flowable.fromIterable(saved);
        }

        throw new IllegalArgumentException("Following items couldn't be saved:" + unprocessed.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    @Override
    public T delete(Object partitionKey, @Nullable Object sortKey) {
        return doWithKey(partitionKey, sortKey, this::delete);
    }

    @Override
    public T delete(T item) {
        publisher.publishEvent(DynamoDbEvent.preRemove(item));
        table.deleteItem(table.keyFrom(item));
        publisher.publishEvent(DynamoDbEvent.postRemove(item));
        return item;
    }

    @Override
    public T delete(Key key) {
        T item = table.tableSchema().mapToItem(key.primaryKeyMap(table.tableSchema()));
        publisher.publishEvent(DynamoDbEvent.preRemove(item));
        table.deleteItem(key);
        publisher.publishEvent(DynamoDbEvent.postRemove(item));
        return item;
    }

    @Override
    public int deleteAll(Flowable<T> items) {
        TableSchema<T> tableSchema = table.tableSchema();
        List<T> deleted = new ArrayList<>();
        List<Key> unprocessed = items.buffer(BATCH_SIZE).map(batchItems -> enhancedClient.batchWriteItem(b -> {
            b.writeBatches(batchItems.stream().map(i -> {
                    publisher.publishEvent(DynamoDbEvent.preRemove(i));
                    deleted.add(i);
                    return WriteBatch.builder(tableSchema.itemType().rawClass()).mappedTableResource(table).addDeleteItem(i).build();
                }
            ).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.unprocessedDeleteItemsForTable(table))).toList().blockingGet();

        if (unprocessed.isEmpty()) {
            deleted.forEach(i -> publisher.publishEvent(DynamoDbEvent.postRemove(i)));
            return deleted.size();
        }

        throw new IllegalArgumentException("Following items couldn't be deleted:" + unprocessed.stream()
            .map(k -> tableSchema.mapToItem(k.keyMap(tableSchema, TableMetadata.primaryIndexName()))).map(Object::toString)
            .collect(Collectors.joining(", ")));
    }

    @Override
    public T get(Object partitionKey, Object sortKey) {
        return doWithKey(partitionKey, sortKey, this::get);
    }

    @Override
    public Flowable<T> getAll(Object partitionKey, Flowable<?> sortKeys) {
        return doWithKeys(partitionKey, sortKeys, this::getAll);
    }

    @Override
    public T get(Key key) {
        T item = table.getItem(key);
        publisher.publishEvent(DynamoDbEvent.postLoad(item));
        return item;
    }

    @Override
    public int count(DetachedQuery<T> query) {
        return query.count(table, attributeConversionHelper);
    }

    @Override
    public int count(DetachedScan<T> scan) {
        return scan.count(table, attributeConversionHelper);
    }

    @Override
    public int count(Object partitionKey, @Nullable Object sortKey) {
        return count(simplePartitionAndSort(partitionKey, sortKey));
    }

    @Override
    public void createTable() {
        Map<String, ProjectionType> types = getProjectionTypes();
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

    private DetachedQuery<T> simplePartitionAndSort(Object partitionKey, Object sortKey) {
        return doWithKey(partitionKey, sortKey, key -> {
            if (key.sortKeyValue().isPresent()) {
                return Builders.query(q -> q.partitionKey(key.partitionKeyValue()).sortKey(s -> s.eq(key.sortKeyValue().get())));
            }
            return Builders.query(q -> q.partitionKey(key.partitionKeyValue()));
        });
    }

    private Flowable<T> getAll(AttributeValue hashKey, Flowable<AttributeValue> rangeKeys) {
        TableSchema<T> tableSchema = table.tableSchema();
        return rangeKeys.buffer(BATCH_SIZE).map(batchRangeKeys -> enhancedClient.batchGetItem(b -> {
            b.readBatches(batchRangeKeys.stream().map(k ->
                ReadBatch.builder(tableSchema.itemType().rawClass()).mappedTableResource(table).addGetItem(Key.builder().partitionValue(hashKey).sortValue(k).build()).build()
            ).collect(Collectors.toList()));
        })).flatMap(r -> Flowable.fromIterable(r.resultsForTable(table)).map(this::postLoad));
    }

    private Map<String, ProjectionType> getProjectionTypes() {
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
            for (String name : indexNames) {
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

    private T postLoad(T i) {
        publisher.publishEvent(DynamoDbEvent.postLoad(i));
        return i;
    }

    private <R> R doWithKey(Object partitionKey, Object sortKey, Function<Key, R> function) {
        String hashKeyName = table.tableSchema().tableMetadata().primaryPartitionKey();
        AttributeValue partitionKeyValue = attributeConversionHelper.convert(table, hashKeyName, partitionKey);

        if (sortKey == null) {
            return function.apply(Key.builder().partitionValue(partitionKeyValue).build());
        }

        String rangeKeyName = table.tableSchema().tableMetadata().primarySortKey().get();
        AttributeValue sortKeyValue = attributeConversionHelper.convert(table, rangeKeyName, sortKey);
        return function.apply(
            Key.builder()
                .partitionValue(partitionKeyValue)
                .sortValue(sortKeyValue)
                .build()
        );
    }

    private <R> Flowable<R> doWithKeys(Object partitionKey, Flowable<?> sortKeys, BiFunction<AttributeValue, Flowable<AttributeValue>, Flowable<R>> function) {
        String hashKeyName = table.tableSchema().tableMetadata().primaryPartitionKey();
        AttributeValue partitionKeyValue = attributeConversionHelper.convert(table, hashKeyName, partitionKey);

        Optional<String> sortKeyName = table.tableSchema().tableMetadata().primarySortKey();
        return function.apply(partitionKeyValue, sortKeys.map(key -> attributeConversionHelper.convert(table, sortKeyName.get(), key)));
    }
}
