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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ScanBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import io.micronaut.core.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Middle-level service for interaction with the DynamoDB.
 *
 * @param <T> the type of the items.
 * @see DynamoDBServiceProvider for obtaining instances of this class.
 */
public interface DynamoDbService<T> {

    int DEFAULT_BATCH_SIZE = 25;

    /**
     * @return the type of the items handled by this service
     */
    Class<T> getItemType();

    /**
     * Returns the low-level {@link DynamoDbTable} instance to execute operations not covered by this service.
     *
     * Warning: no events are triggered while using the low level API.
     *
     * @return the low-level {@link DynamoDbTable} instance to execute operations not covered by this service
     */
    DynamoDbTable<T> getTable();

    /**
     * Executes the prepared query and returns the items matching the query.
     * @param query the query
     * @return the items matching the given query
     */
    Publisher<T> query(DetachedQuery<T> query);

    /**
     * Defines the query using the query builder and returns the items matching the query.
     * @param query the query definition
     * @return the items matching the given query
     */
    default Publisher<T> query(Consumer<QueryBuilder<T>> query) {
        return query(Builders.query(query));
    }

    /**
     * Executes the prepared scan (non-index query) and returns the items matching the scan.
     * @param scan the scan
     * @return the items matching the scan
     */
    Publisher<T> scan(DetachedScan<T> scan);

    /**
     * Defines the scan (non-index query) using the scan builder and returns the items matching the scan.
     * @param scan the scan definition
     * @return the items matching the scan
     */
    default Publisher<T> scan(Consumer<ScanBuilder<T>> scan) {
        return scan(Builders.scan(scan));
    }

    /**
     * Finds all the items for given partition key.
     *
     * If the sort key is present it either returns {@link Publisher} with single item or an empty one.
     *
     * @param partitionKey the partition key
     * @param sortKey the sort key
     * @return flowable of all items with given partition key and sort key (if present)
     */
    Publisher<T> findAll(Object partitionKey, @Nullable Object sortKey);

    /**
     * Finds all the items for given partition key.
     *
     * @param partitionKey the partition key
     * @return flowable of all items with given partition key
     */
    default Publisher<T> findAll(Object partitionKey) {
        return findAll(partitionKey, null);
    }

    /**
     * Updates the item using the given update definition and returns the result according it's return definition.
     * @param update the update definition
     * @return the result according to update definition's return settings.
     */
    <R> R update(DetachedUpdate<T, R> update);

    /**
     * Updates the item using the given update definition and returns the result according it's return definition.
     * @param update the update definition
     * @return the result according to update definition's return settings.
     */
    default <R> R update(Function<UpdateBuilder<T, T>, UpdateBuilder<T, R>> update) {
        return update(Builders.update(update));
    }

    int updateAll(Publisher<T> items, UpdateBuilder<T, ?> update);

    default <R> int updateAll(Publisher<T> items, Function<UpdateBuilder<T, T>, UpdateBuilder<T, R>> update) {
        return updateAll(items, Builders.update(update));
    }

    T save(T entity);

    /**
     * Saves all the items from the given publisher.
     * @param itemsToSave the items to save
     * @param batchSize the batch size
     * @return the saved items
     */
    Publisher<T> saveAll(Publisher<T> itemsToSave, int batchSize);

    default Publisher<T> saveAll(Publisher<T> itemsToSave) {
        return saveAll(itemsToSave, DEFAULT_BATCH_SIZE);
    }

    T delete(Object partitionKey, @Nullable Object sortKey);

    T delete(T item);

    T delete(Key key);

    /**
     * Deletes all the items from the given publisher.
     * @param items the items to delete
     * @param batchSize the batch size
     * @return the number of deleted items
     */
    int deleteAll(Publisher<T> items, int batchSize);

    default int deleteAll(Publisher<T> items) {
        return deleteAll(items, DEFAULT_BATCH_SIZE);
    }

    T get(Object partitionKey, Object sortKey);

    Publisher<T> getAll(Object partitionKey, Publisher<?> sortKeys);

    /**
     * Finds all the items for given partition key.
     *
     * @param partitionKeys the partition keys
     * @param batchSize the batch size, max 25
     * @return flowable of all items with given partition keys
     */
    Publisher<T> getAll(Publisher<?> partitionKeys, int batchSize);

    default Publisher<T> getAll(Publisher<?> partitionKeys) {
        return getAll(partitionKeys, DEFAULT_BATCH_SIZE);
    }

    T get(Key key);

    int count(DetachedQuery<T> query);

    default int countUsingQuery(Consumer<QueryBuilder<T>> query) {
        return count(Builders.query(query));
    }

    int count(DetachedScan<T> scan);

    default int countUsingScan(Consumer<ScanBuilder<T>> scan) {
        return count(Builders.scan(scan));
    }

    int count(Object partitionKey, @Nullable Object sortKey);

    void createTable();
}
