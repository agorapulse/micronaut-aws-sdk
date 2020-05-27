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
import io.reactivex.Flowable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface DynamoDbService<T> {

    Class<T> getItemType();

    TableSchema<T> getTableSchema();

    Flowable<T> query(DetachedQuery<T> query);

    default Flowable<T> query(Consumer<QueryBuilder<T>> query) {
        return query(Builders.query(query));
    }

    Flowable<T> scan(DetachedScan<T> query);

    default Flowable<T> scan(Consumer<ScanBuilder<T>> scan) {
        return scan(Builders.scan(scan));
    }

    Flowable<T> findAll(Object partitionKey, Object sortKey);

    Object update(DetachedUpdate<T> update);

    default Object update(Consumer<UpdateBuilder<T>> update) {
        return update(Builders.update(update));
    }

    int updateAll(Flowable<T> items, UpdateBuilder<T> update);

    default int updateAll(Flowable<T> items, Consumer<UpdateBuilder<T>> update) {
        return updateAll(items, Builders.update(update));
    }

    T save(T entity);

    Flowable<T> saveAll(Flowable<T> itemsToSave);

    T delete(Object partitionKey, @Nullable Object sortKey);

    T delete(T item);

    T delete(Key key);

    int deleteAll(Flowable<T> items);

    T get(Object partitionKey, Object sortKey);

    Flowable<T> getAll(Object partitionKey, Flowable<?> sortKeys);

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
