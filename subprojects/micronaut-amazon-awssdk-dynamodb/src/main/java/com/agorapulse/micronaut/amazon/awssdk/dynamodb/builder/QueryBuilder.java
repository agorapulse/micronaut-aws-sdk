/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Builder for DynamoDB queries.
 * @param <T> type of the DynamoDB entity
 */
public interface QueryBuilder<T> extends DetachedQuery<T> {

    /**
     * Sort the results by the sort key
     * @param sort the sort keyword
     * @return self
     * @deprecated use {@link #order(Builders.Sort)} instead
     */
    @Deprecated
    default QueryBuilder<T> sort(Builders.Sort sort) {
        return order(sort);
    }

    /**
     * Orders the results by the sort key
     * @param sort the order keyword
     * @return self
     */
    QueryBuilder<T> order(Builders.Sort sort);

    /**
     * Demand consistent reads.
     * @param read the read keyword
     * @return self
     */
    QueryBuilder<T> consistent(Builders.Read read);

    /**
     * Demand inconsistent reads.
     * @param read the read keyword
     * @return self
     */
    QueryBuilder<T> inconsistent(Builders.Read read);

    /**
     * Select the index on which this query will be executed.
     * @param name the name of the index to be used
     * @return self
     */
    QueryBuilder<T> index(String name);

    /**
     * Sets the partition key value for the query.
     *
     * This parameter is required for every query.
     *
     * @param key the partition key of the query
     * @return self
     * @deprecated use {@link #partitionKey(Object)} instead
     */
    @Deprecated
    default QueryBuilder<T> hash(Object key) {
        return partitionKey(key);
    }

    /**
     * Sets the partition key value for the query.
     *
     * This parameter is required for every query.
     *
     * @param key the partition key of the query
     * @return self
     */
    QueryBuilder<T> partitionKey(Object key);

    /**
     * Creates a sort key condition.
     * @param conditions consumer to build the conditions
     * @return self
     */
    QueryBuilder<T> sortKey(Consumer<KeyConditionCollector<T>> conditions);

    /**
     * Creates a sort key condition.
     * @param conditions consumer to build the conditions
     * @return self
     * @deprecated use {@link #sortKey(Consumer)} instead
     */
    @Deprecated
    default QueryBuilder<T> range(Consumer<KeyConditionCollector<T>> conditions) {
        return sortKey(conditions);
    }

    /**
     * One or more filter conditions.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    QueryBuilder<T> filter(Consumer<FilterConditionCollector<T>> conditions);

    /**
     * Sets the desired pagination of the queries.
     *
     * This only sets the optimal pagination of the queries and does not limit the number of items returned.
     *
     * Use <code>{@link io.reactivex.Flowable#take(long)}</code> to limit the number results returned from the query.
     *
     * @param page number of entities loaded by one query request (not a number of total entities returned)
     * @return self
     */
    QueryBuilder<T> page(int page);

    /**
     * Sets the maximum number of items to be returned from the queries.
     *
     * This is a shortcut for calling <code>{@link io.reactivex.Flowable#take(long)}</code> on the result Flowable.
     *
     * @param max the maximum number of items returned
     * @return self
     */
    QueryBuilder<T> limit(int max);


    /**
     * Sets the query offset by defining the exclusive start value.
     * @param lastEvaluatedKey exclusive start value
     * @return self
     */
    QueryBuilder<T> lastEvaluatedKey(Object lastEvaluatedKey);

    /**
     * Configures the native query expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native query expression
     * @return self
     */
    QueryBuilder<T> configure(Consumer<QueryEnhancedRequest.Builder> configurer);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    QueryBuilder<T> only(Collection<String> propertyPaths);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    default QueryBuilder<T> only(String... propertyPaths) {
        return only(Arrays.asList(propertyPaths));
    }

}
