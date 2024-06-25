/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondaryPartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondarySortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.events.DynamoDbEvent;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultAsyncDynamoDbService<T> implements AsyncDynamoDbService<T> {

    private static final int BATCH_SIZE = 25;

    private final Class<T> itemType;
    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncClient client;
    private final AttributeConversionHelper attributeConversionHelper;
    private final ApplicationEventPublisher<DynamoDbEvent> publisher;
    private final DynamoDbAsyncTable<T> table;

    public DefaultAsyncDynamoDbService(
        Class<T> itemType,
        DynamoDbEnhancedAsyncClient enhancedClient,
        DynamoDbAsyncClient client,
        AttributeConversionHelper attributeConversionHelper,
        ApplicationEventPublisher<DynamoDbEvent> publisher,
        DynamoDbAsyncTable<T> table
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
    public DynamoDbAsyncTable<T> getTable() {
        return table;
    }

    @Override
    public Publisher<T> query(DetachedQuery<T> query) {
        return Flux.from(query.query(table, attributeConversionHelper)).map(this::postLoad);
    }

    @Override
    public Publisher<T> scan(DetachedScan<T> scan) {
        return Flux.from(scan.scan(table, attributeConversionHelper)).map(this::postLoad);
    }

    @Override
    public Publisher<T> findAll(Object partitionKey, Object sortKey) {
        return Flux.from(simplePartitionAndSort(partitionKey, sortKey).query(table, attributeConversionHelper)).map(this::postLoad);
    }

    @Override
    public <R> Publisher<R> update(DetachedUpdate<T, R> update) {
        return update.update(table, client, attributeConversionHelper, publisher);
    }

    @Override
    public <R> Publisher<R> updateAll(Publisher<T> items, UpdateBuilder<T, R> update) {
        // there is no batch update API, we can do batch updates in transaction but in that case it would cause
        // doubling the writes

        BeanIntrospection<T> introspection = EntityIntrospection.getBeanIntrospection(table);
        TableMetadata tableMetadata = table.tableSchema().tableMetadata();

        return Flux.from(items).map(this::postLoad).flatMap(entity -> {
            introspection.getProperty(tableMetadata.primaryPartitionKey()).ifPresent(p -> update.partitionKey(p.get(entity)));
            tableMetadata.primarySortKey().flatMap(introspection::getProperty).ifPresent(p -> update.sortKey(p.get(entity)));

            return update.update(table, client, attributeConversionHelper, publisher);
        });
    }

    @Override
    public Publisher<T> save(T entity) {
        publisher.publishEvent(DynamoDbEvent.prePersist(entity));
        return Mono.fromFuture(table.updateItem(entity))
            .flatMap(updated ->
                Mono.fromCallable(() -> {
                    publisher.publishEvent(DynamoDbEvent.postPersist(updated));
                    return updated;
                })
            );
    }

    @Override
    public Publisher<T> saveAll(Publisher<T> itemsToSave) {
        return Flux.from(itemsToSave)
            .buffer(BATCH_SIZE)
            .flatMap(batchItems ->
                Mono.fromFuture(enhancedClient.batchWriteItem(b -> {
                    List<WriteBatch> writeBatches = batchItems.stream().map(i -> {
                        publisher.publishEvent(DynamoDbEvent.prePersist(i));
                        return WriteBatch.builder(table.tableSchema().itemType().rawClass()).mappedTableResource(table).addPutItem(i).build();
                    }).toList();
                    b.writeBatches(writeBatches);
                })).zipWith(Mono.just(batchItems))
            )
            .flatMap(r -> {
                List<T> unprocesseded = r.getT1().unprocessedPutItemsForTable(table);
                if (unprocesseded.isEmpty()) {
                    return Flux.fromIterable(r.getT2()).doOnNext(i -> publisher.publishEvent(DynamoDbEvent.postPersist(i)));
                }
                return Flux.error(new IllegalArgumentException("Following items couldn't be saved:" + unprocesseded.stream().map(Object::toString).collect(Collectors.joining(", "))));
            });
    }

    @Override
    public Publisher<T> delete(Object partitionKey, @Nullable Object sortKey) {
        return doWithKey(partitionKey, sortKey, this::delete);
    }

    @Override
    public Publisher<T> delete(T item) {
        publisher.publishEvent(DynamoDbEvent.preRemove(item));
        return Mono.fromFuture(table.deleteItem(table.keyFrom(item))).map(deletedItem -> {
            publisher.publishEvent(DynamoDbEvent.postRemove(deletedItem));
            return deletedItem;
        });
    }

    @Override
    public Publisher<T> delete(Key key) {
        T item = table.tableSchema().mapToItem(key.primaryKeyMap(table.tableSchema()));
        publisher.publishEvent(DynamoDbEvent.preRemove(item));
        return Mono.fromFuture(table.deleteItem(key)).map(deletedItem -> {
            publisher.publishEvent(DynamoDbEvent.postRemove(deletedItem));
            return deletedItem;
        });
    }

    @Override
    public Publisher<T> deleteAll(Publisher<T> items) {
        TableSchema<T> tableSchema = table.tableSchema();
        return Flux.from(items)
            .buffer(BATCH_SIZE)
            .flatMap(batchItems ->
                Mono.fromFuture(enhancedClient.batchWriteItem(b -> {
                    List<WriteBatch> writeBatches = batchItems.stream().map(i -> {
                        publisher.publishEvent(DynamoDbEvent.preRemove(i));
                        return WriteBatch.builder(table.tableSchema().itemType().rawClass()).mappedTableResource(table).addDeleteItem(i).build();
                    }).toList();
                    b.writeBatches(writeBatches);
                })).zipWith(Mono.just(batchItems))
            )
            .flatMap(r -> {
                List<Key> unprocesseded = r.getT1().unprocessedDeleteItemsForTable(table);
                if (unprocesseded.isEmpty()) {
                    r.getT2().forEach(i -> publisher.publishEvent(DynamoDbEvent.postRemove(i)));
                    return Flux.fromIterable(r.getT2());
                }
                return Flux.error(new IllegalArgumentException("Following items couldn't be deleted:" + unprocesseded.stream()
                    .map(k -> tableSchema.mapToItem(k.keyMap(tableSchema, TableMetadata.primaryIndexName()))).map(Object::toString)
                    .collect(Collectors.joining(", "))));
            });
    }

    @Override
    public Publisher<T> get(Object partitionKey, Object sortKey) {
        return doWithKey(partitionKey, sortKey, this::get);
    }

    @Override
    public Publisher<T> getAll(Object partitionKey, Publisher<?> sortKeys) {
        return doWithKeys(partitionKey, sortKeys, this::getAll);
    }

    @Override
    public Publisher<T> get(Key key) {
        return Mono.fromFuture(table.getItem(key)).map(this::postLoad);
    }

    @Override
    public Publisher<Long> count(DetachedQuery<T> query) {
        return query.count(table, attributeConversionHelper);
    }

    @Override
    public Publisher<Long> count(DetachedScan<T> scan) {
        return scan.count(table, attributeConversionHelper);
    }

    @Override
    public Publisher<Long> count(Object partitionKey, @Nullable Object sortKey) {
        return count(simplePartitionAndSort(partitionKey, sortKey));
    }

    @Override
    public Publisher<Boolean> createTable() {
        Map<String, ProjectionType> types = getProjectionTypes();
        TableMetadata tableMetadata = table.tableSchema().tableMetadata();
        return Mono.fromFuture(table.createTable(b -> {
            List<EnhancedLocalSecondaryIndex> localSecondaryIndices = new ArrayList<>();
            List<EnhancedGlobalSecondaryIndex> globalSecondaryIndices = new ArrayList<>();

            tableMetadata.indices().forEach(i -> {
                if (TableMetadata.primaryIndexName().equals(i.name())) {
                    return;
                }
                ProjectionType type = types.getOrDefault(i.name(), ProjectionType.KEYS_ONLY);
                if (tableMetadata.primaryPartitionKey().equals(tableMetadata.indexPartitionKey(i.name()))) {
                    localSecondaryIndices.add(EnhancedLocalSecondaryIndex.create(i.name(), Projection.builder().projectionType(type).build()));
                } else {
                    globalSecondaryIndices.add(EnhancedGlobalSecondaryIndex.builder().indexName(i.name()).projection(Projection.builder().projectionType(type).build()).build());
                }
            });

            if (!localSecondaryIndices.isEmpty()) {
                b.localSecondaryIndices(localSecondaryIndices);
            }

            if (!globalSecondaryIndices.isEmpty()) {
                b.globalSecondaryIndices(globalSecondaryIndices);
            }
        })).then(Mono.just(true)).onErrorReturn(false);
    }

    private DetachedQuery<T> simplePartitionAndSort(Object partitionKey, Object sortKey) {
        return doWithKey(partitionKey, sortKey, key -> {
            if (key.sortKeyValue().isPresent()) {
                return Builders.query(q -> q.partitionKey(key.partitionKeyValue()).sortKey(s -> s.eq(key.sortKeyValue().get())));
            }
            return Builders.query(q -> q.partitionKey(key.partitionKeyValue()));
        });
    }

    private Publisher<T> getAll(AttributeValue hashKey, Publisher<AttributeValue> rangeKeys) {
        TableSchema<T> tableSchema = table.tableSchema();
        Map<AttributeValue, Integer> order = new ConcurrentHashMap<>();
        AtomicInteger counter = new AtomicInteger();
        Comparator<T> comparator = Comparator.comparingInt(i -> order.getOrDefault(tableSchema.attributeValue(i, tableSchema.tableMetadata().primarySortKey().get()), 0));

        return Flux.from(rangeKeys).buffer(BATCH_SIZE).map(batchRangeKeys -> enhancedClient.batchGetItem(b -> b.readBatches(batchRangeKeys.stream().map(k -> {
                order.put(k, counter.getAndIncrement());
                return ReadBatch.builder(tableSchema.itemType().rawClass()).mappedTableResource(table).addGetItem(Key.builder().partitionValue(hashKey).sortValue(k).build()).build();
            }
        ).toList()))).flatMap(r -> Flux.from(r.resultsForTable(table)).map(this::postLoad)).sort(comparator);
    }

    private Map<String, ProjectionType> getProjectionTypes() {
        Map<String, ProjectionType> types = new HashMap<>();

        BeanIntrospection<T> introspection = EntityIntrospection.getBeanIntrospection(table);
        introspection.getBeanProperties().forEach(p -> {
            AnnotationValue<com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection> projectionAnnotation = p.getAnnotation(com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection.class);

            if (projectionAnnotation == null) {
                return;
            }

            Collection<String> indexNames = new ArrayList<>();

            indexNames.addAll(collectIndicesFromAnnotation(p, DynamoDbSecondarySortKey.class));
            indexNames.addAll(collectIndicesFromAnnotation(p, SecondarySortKey.class));
            indexNames.addAll(collectIndicesFromAnnotation(p, DynamoDbSecondaryPartitionKey.class));
            indexNames.addAll(collectIndicesFromAnnotation(p, SecondaryPartitionKey.class));

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

    private List<String> collectIndicesFromAnnotation(BeanProperty<T, Object> p, Class<? extends Annotation> indexAnnotationClass) {
        return p.findAnnotation(indexAnnotationClass).map(anno -> Arrays.asList(anno.stringValues("indexNames"))).orElse(Collections.emptyList());
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

    private <R> Publisher<R> doWithKeys(Object partitionKey, Publisher<?> sortKeys, BiFunction<AttributeValue, Publisher<AttributeValue>, Publisher<R>> function) {
        String hashKeyName = table.tableSchema().tableMetadata().primaryPartitionKey();
        AttributeValue partitionKeyValue = attributeConversionHelper.convert(table, hashKeyName, partitionKey);

        Optional<String> sortKeyName = table.tableSchema().tableMetadata().primarySortKey();
        return function.apply(partitionKeyValue, Flux.from(sortKeys).map(key -> attributeConversionHelper.convert(table, sortKeyName.get(), key)));
    }
}
