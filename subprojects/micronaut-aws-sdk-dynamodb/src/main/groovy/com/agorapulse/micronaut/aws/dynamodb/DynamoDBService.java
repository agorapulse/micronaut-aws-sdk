/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.*;

/**
 * The middle-level API for working with DynamoDB tables.
 *
 * Using declarative services using {@link com.agorapulse.micronaut.aws.dynamodb.annotation.Service} is preferred.
 *
 * @param <T> The type of the DynamoDB entity.
 */
public interface DynamoDBService<T> {

    int DEFAULT_QUERY_LIMIT = 20;
    int DEFAULT_COUNT_LIMIT = 100;
    int BATCH_DELETE_LIMIT = 100;
    int WRITE_BATCH_SIZE = 100; // Max number of elements to write at once in DynamoDB (mixed tables)

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    int count(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator, Map settings);

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @return
     */
    default int count(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator) {
        return count(hashKey, rangeKeyName, rangeKeyValue, operator, Collections.emptyMap());
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @return
     */
    default int count(Object hashKey, String rangeKeyName, Object rangeKeyValue) {
        return count(hashKey, rangeKeyName, rangeKeyValue, ComparisonOperator.EQ);
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyValue
     * @return
     */
    default int count(Object hashKey, Object rangeKeyValue) {
        return count(hashKey, getRangeKeyName(), rangeKeyValue);
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @return
     */
    default int count(Object hashKey) {
        return count(hashKey, null);
    }

    default int countByDates(Object hashKey, String rangeKeyName, Date after, Date before, Date maxAfterDate) {
        Map<String, Object> rangeKeyDates = new HashMap<>(2);
        rangeKeyDates.put("after", after);
        rangeKeyDates.put("before", before);
        return countByDates(hashKey, rangeKeyName, rangeKeyDates, Collections.singletonMap("maxAfterDate", maxAfterDate));
    }

    default int countByDates(Object hashKey, String rangeKeyName, Date after, Date before) {
        return countByDates(hashKey, rangeKeyName, after, before, null);
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     * - maxAfterDate (default to null)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @param settings
     * @return
     */
    int countByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates, Map settings);

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     * - maxAfterDate (default to null)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @return
     */
    default int countByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates) {
        return countByDates(hashKey, rangeKeyName, rangeKeyDates, Collections.emptyMap());
    }

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    int countByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map settings);

    /**
     * Optional settings:
     * - consistentRead (default to false)
     * - limit (default to DEFAULT_COUNT_LIMIT)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @return
     */
    default int countByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions) {
        return countByConditions(hashKey, rangeKeyConditions, Collections.emptyMap());
    }

    /**
     * Create the DynamoDB table for the given Class.
     */
    CreateTableResult createTable(Long readCapacityUnits, Long writeCapacityUnits);

    /**
     * Create the DynamoDB table for the given Class.
     *
     */
    default CreateTableResult createTable(Long readCapacityUnits) {
        return createTable(readCapacityUnits, 5L);
    }

    /**
     * Create the DynamoDB table for the given Class.
     *
     */
    default CreateTableResult createTable() {
        return createTable(10L);
    }

    /**
     * Decrement a count with an atomic operation
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeIncrement
     * @return
     */
    default Integer decrement(Object hashKey, Object rangeKey, String attributeName, int attributeIncrement) {
        return increment(hashKey, rangeKey, attributeName, -attributeIncrement);
    }

    /**
     * Decrement a count with an atomic operation
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @return
     */
    default Integer decrement(Object hashKey, Object rangeKey, String attributeName) {
        return decrement(hashKey, rangeKey, attributeName, 1);
    }

    /**
     * Decrement a count with an atomic operation
     *
     * @param hashKey
     * @param attributeName
     * @return
     */
    default Integer decrement(Object hashKey, String attributeName) {
        return decrement(hashKey, null, attributeName, 1);
    }

    /**
     * Delete item by IDs.
     *
     * @param hashKey  hash key of the item to delete
     * @param rangeKey range key of the item to delete
     * @param settings settings
     */
    void delete(Object hashKey, Object rangeKey, Map settings);

    /**
     * Delete item by IDs.
     *
     * @param hashKey  hash key of the item to delete
     * @param rangeKey range key of the item to delete
     */
    default void delete(Object hashKey, Object rangeKey) {
        delete(hashKey, rangeKey, Collections.emptyMap());
    }

    /**
     * Delete item from Java object
     *
     * @param item
     * @param settings
     */
    default void delete(T item, Map settings) {
        deleteAll(Collections.singletonList(item), settings);
    }

    /**
     * Delete item from Java object
     *
     * @param item
     */
    default void delete(T item) {
        delete(item, Collections.emptyMap());
    }

    /**
     * Delete item from Java object
     *
     * @param id
     */
    default void deleteByHash(Object id) {
        delete(id, null, Collections.emptyMap());
    }

    /**
     * Delete a list of items from DynamoDB.
     *
     * @param itemsToDelete a list of objects to delete
     * @param settings      settings
     */
    void deleteAll(List<T> itemsToDelete, Map settings);

    /**
     * Delete a list of items from DynamoDB.
     *
     * @param itemsToDelete a list of objects to delete
     */
    default void deleteAll(List<T> itemsToDelete) {
        deleteAll(itemsToDelete, Collections.emptyMap());
    }

    /**
     * Delete all items for a given hashKey
     *
     * @param hashKey
     * @param settings
     * @return
     */
    default int deleteAll(Object hashKey, Map settings) {
        return deleteAllByConditions(hashKey, Collections.emptyMap(), settings);
    }

    /**
     * Delete all items for a given hashKey
     *
     * @param hashKey
     * @return
     */
    default int deleteAll(Object hashKey) {
        return deleteAll(hashKey, Collections.emptyMap());
    }

    /**
     * Delete all items for a given hashKey and a rangeKey condition
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    int deleteAll(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator, Map settings);

    /**
     * Delete all items for a given hashKey and a rangeKey condition
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @return
     */
    default int deleteAll(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator) {
        return deleteAll(hashKey, rangeKeyName, rangeKeyValue, operator, Collections.emptyMap());
    }

    /**
     * Delete all items for a given hashKey and a rangeKey condition
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @return
     */
    default int deleteAll(Object hashKey, String rangeKeyName, Object rangeKeyValue) {
        return deleteAll(hashKey, rangeKeyName, rangeKeyValue, ComparisonOperator.BEGINS_WITH);
    }

    /**
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @param indexName
     * @return
     */
    int deleteAllByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map settings, String indexName);

    int deleteAllByConditions(DynamoDBQueryExpression query, Map settings);

    /**
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    default int deleteAllByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map settings) {
        return deleteAllByConditions(hashKey, rangeKeyConditions, settings, null);
    }

    /**
     * @param hashKey
     * @param rangeKeyConditions
     * @return
     */
    default int deleteAllByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions) {
        return deleteAllByConditions(hashKey, rangeKeyConditions, Collections.emptyMap());
    }

    /**
     * Load an item
     *
     * @param hashKey
     * @param rangeKey
     * @return
     */
    T get(Object hashKey, Object rangeKey);

    /**
     * Load an item
     *
     * @param hashKey
     * @return
     */
    default T get(Object hashKey) {
        return get(hashKey, null);
    }

    /**
     * Retrieve batched items corresponding to a list of item IDs, in the same order.
     * Example: items = twitterItemDBService.getAll(1, [1, 2]).
     *
     * @param hashKey  Hash Key of the items to retrieve
     * @param settings only used for setting throttle/readCapacityUnit when getting large sets
     * @return a list of DynamoDBItem
     */
    List<T> getAll(Object hashKey, List rangeKeys, Map settings);

    /**
     * Retrieve batched items corresponding to a list of item IDs, in the same order.
     * Example: items = twitterItemDBService.getAll(1, [1, 2]).
     *
     * @param hashKey  Hash Key of the items to retrieve
     * @return a list of DynamoDBItem
     */
    default List<T> getAll(Object hashKey, List rangeKeys) {
        return getAll(hashKey, rangeKeys, Collections.emptyMap());
    }

    /**
     * Increment a count with an atomic operation
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeIncrement
     * @return
     */
    Integer increment(Object hashKey, Object rangeKey, String attributeName, int attributeIncrement);

    /**
     * Increment a count with an atomic operation
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @return
     */
    default Integer increment(Object hashKey, Object rangeKey, String attributeName) {
        return increment(hashKey, rangeKey, attributeName, 1);
    }

    /**
     * Increment a count with an atomic operation
     *
     * @param hashKey
     * @param attributeName
     * @return
     */
    default Integer increment(Object hashKey, String attributeName) {
        return increment(hashKey, null, attributeName, 1);
    }

    T getNewInstance();

    PaginatedQueryList<T> query(DynamoDBQueryExpression queryExpression);

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param settings
     * @return
     */
    default QueryResultPage<T> query(Object hashKey, Map settings) {
        return queryByConditions(hashKey, Collections.emptyMap(), settings);
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @return
     */
    default QueryResultPage<T> query(Object hashKey) {
        return query(hashKey, Collections.emptyMap());
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @param settings
     * @return
     */
    QueryResultPage<T> query(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator, Map settings);

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @param operator
     * @return
     */
    default QueryResultPage<T> query(Object hashKey, String rangeKeyName, Object rangeKeyValue, ComparisonOperator operator) {
        return query(hashKey, rangeKeyName, rangeKeyValue, operator, Collections.emptyMap());
    }

    default QueryResultPage<T> query(Object hashKey, Object rangeKeyValue) {
        return query(hashKey, getRangeKeyName(), rangeKeyValue);
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyValue
     * @return
     */
    default QueryResultPage<T> query(Object hashKey, String rangeKeyName, Object rangeKeyValue) {
        return query(hashKey, rangeKeyName, rangeKeyValue, ComparisonOperator.EQ);
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     * - throttle insert sleeps during execution to avoid reaching provisioned read throughput (default to false)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    QueryResultPage<T> queryByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map settings, String indexName);

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     * - throttle insert sleeps during execution to avoid reaching provisioned read throughput (default to false)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @param settings
     * @return
     */
    default QueryResultPage<T> queryByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions, Map settings) {
        return queryByConditions(hashKey, rangeKeyConditions, settings, null);
    }

    /**
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - returnAll disable paging to return all items, WARNING: can be expensive in terms of throughput (default to false)
     * - scanIndexForward (default to false)
     * - throttle insert sleeps during execution to avoid reaching provisioned read throughput (default to false)
     *
     * @param hashKey
     * @param rangeKeyConditions
     * @return
     */
    default QueryResultPage<T> queryByConditions(Object hashKey, Map<String, Condition> rangeKeyConditions) {
        return queryByConditions(hashKey, rangeKeyConditions, Collections.emptyMap());
    }

    /**
     * Query by dates with 'after' and/or 'before' range value
     * 1) After a certain date : [after: new Date()]
     * 2) Before a certain date : [before: new Date()]
     * 3) Between provided dates : [after: new Date() + 1, before: new Date()]
     * <p>
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - maxAfterDate (default to null)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @param settings
     * @return
     */
    QueryResultPage<T> queryByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates, Map settings);

    /**
     * Query by dates with 'after' and/or 'before' range value
     * 1) After a certain date : [after: new Date()]
     * 2) Before a certain date : [before: new Date()]
     * 3) Between provided dates : [after: new Date() + 1, before: new Date()]
     * <p>
     * Optional settings:
     * - batchGetDisabled (only when secondary indexes are used, useful for count when all item attributes are not required)
     * - consistentRead (default to false)
     * - exclusiveStartKey a map with the rangeKey (ex: [id: 2555]), with optional indexRangeKey when using LSI (ex.: [id: 2555, totalCount: 45])
     * - limit
     * - maxAfterDate (default to null)
     * - scanIndexForward (default to false)
     *
     * @param hashKey
     * @param rangeKeyName
     * @param rangeKeyDates
     * @return
     */
    default QueryResultPage<T> queryByDates(Object hashKey, String rangeKeyName, Map rangeKeyDates) {
        return queryByDates(hashKey, rangeKeyName, rangeKeyDates, Collections.emptyMap());
    }

    default QueryResultPage<T> queryByDates(Object hashKey, String rangeKeyName, Date after, Date before) {
        return queryByDates(hashKey, rangeKeyName, after, before, null);
    }

    default QueryResultPage<T> queryByDates(Object hashKey, String rangeKeyName, Date after, Date before, Date maxAfterDate) {
        Map<String, Object> rangeKeyDates = new HashMap<>(2);
        rangeKeyDates.put("after", after);
        rangeKeyDates.put("before", before);
        return queryByDates(hashKey, rangeKeyName, rangeKeyDates, Collections.singletonMap("maxAfterDate", maxAfterDate));
    }

    /**
     * Save an item.
     *
     * @param item     the item to save
     * @param settings settings
     * @return the Item after it's been saved
     */
    default T save(T item, Map settings) {
        return saveAll(Collections.singletonList(item), settings).iterator().next();
    }

    /**
     * Save an item.
     *
     * @param item the item to save
     * @return the Item after it's been saved
     */
    default T save(T item) {
        return save(item, Collections.emptyMap());
    }

    /**
     * Save a list of objects in DynamoDB.
     *
     * @param itemsToSave a list of objects to save
     * @param settings    settings
     */
    List<T> saveAll(List<T> itemsToSave, Map settings);

    /**
     * Save a list of objects in DynamoDB.
     *
     * @param itemsToSave a list of objects to save
     */
    default List<T> saveAll(List<T> itemsToSave) {
        return saveAll(itemsToSave, Collections.emptyMap());
    }

    /**
     * Save a list of objects in DynamoDB.
     *
     * @param itemsToSave a list of objects to save
     */
    default List<T> saveAll(T... itemsToSave) {
        return saveAll(Arrays.asList(itemsToSave));
    }

    /**
     * Delete a single item attribute
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @return
     */
    UpdateItemResult deleteItemAttribute(Object hashKey, Object rangeKey, String attributeName);

    default UpdateItemResult deleteItemAttribute(Object hashKey, String attributeName) {
        return deleteItemAttribute(hashKey, null, attributeName);
    }


    /**
     * Update a single item attribute
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeValue
     * @param action
     * @return
     */
    UpdateItemResult updateItemAttribute(Object hashKey, Object rangeKey, String attributeName, Object attributeValue, AttributeAction action);

    /**
     * Update a single item attribute
     *
     * @param hashKey
     * @param rangeKey
     * @param attributeName
     * @param attributeValue
     * @return
     */
    default UpdateItemResult updateItemAttribute(Object hashKey, Object rangeKey, String attributeName, Object attributeValue) {
        return updateItemAttribute(hashKey, rangeKey, attributeName, attributeValue, AttributeAction.PUT);
    }

    boolean isIndexRangeKey(String rangeName);

    String getHashKeyName();
    Class<?> getHashKeyClass();
    String getRangeKeyName();
    Class<?> getRangeKeyClass();

}
