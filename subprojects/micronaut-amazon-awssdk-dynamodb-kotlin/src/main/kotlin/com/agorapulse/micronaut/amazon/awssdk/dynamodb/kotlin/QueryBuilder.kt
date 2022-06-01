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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.KeyConditionCollector
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

class QueryBuilder<T>(private val delegate: com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder<T>) {

    val asc = Builders.Sort.ASC
    val desc = Builders.Sort.DESC
    val read = Builders.Read.READ

    /**
     * Orders the results by the sort key
     * @param sort the order keyword
     * @return self
     */
    fun order(sort: Builders.Sort): QueryBuilder<T> {
        delegate.order(sort)
        return this
    }

    /**
     * Demand consistent reads.
     * @param read the read keyword
     * @return self
     */
    fun consistent(read: Builders.Read): QueryBuilder<T> {
        delegate.consistent(read)
        return this
    }

    /**
     * Demand inconsistent reads.
     * @param read the read keyword
     * @return self
     */
    fun inconsistent(read: Builders.Read): QueryBuilder<T> {
        delegate.inconsistent(read)
        return this
    }

    /**
     * Select the index on which this query will be executed.
     * @param name the name of the index to be used
     * @return self
     */
    fun index(name: String): QueryBuilder<T> {
        delegate.index(name)
        return this
    }

    /**
     * Sets the partition key value for the query.
     *
     * This parameter is required for every query.
     *
     * @param key the partition key of the query
     * @return self
     */
    fun partitionKey(key: Any??): QueryBuilder<T> {
        delegate.partitionKey(key)
        return this
    }

    /**
     * Creates a sort key condition.
     * @param conditions consumer to build the conditions
     * @return self
     */
    fun sortKey(conditions: KeyConditionCollector<T>.() -> KeyConditionCollector<T>): QueryBuilder<T> {
        delegate.sortKey { conditions(it) }
        return this
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
    fun filter(conditions: FilterConditionCollector<T>.() -> FilterConditionCollector<T>): QueryBuilder<T> {
        delegate.filter { conditions(FilterConditionCollector(it)) }
        return this
    }

    /**
     * Sets the desired pagination of the queries.
     *
     * This only sets the optimal pagination of the queries and does not limit the number of items returned.
     *
     * @param page number of entities loaded by one query request (not a number of total entities returned)
     * @return self
     */
    fun page(page: Int): QueryBuilder<T> {
        delegate.page(page)
        return this
    }

    /**
     * Sets the maximum number of items to be returned from the queries.
     *
     * @param max the maximum number of items returned
     * @return self
     */
    fun limit(max: Int): QueryBuilder<T> {
        delegate.limit(max)
        return this
    }

    /**
     * Sets the query offset by defining the exclusive start value.
     * @param lastEvaluatedKey exclusive start value
     * @return self
     */
    fun lastEvaluatedKey(lastEvaluatedKey: Any?): QueryBuilder<T> {
        delegate.lastEvaluatedKey(lastEvaluatedKey)
        return this
    }

    /**
     * Configures the native query expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native query expression
     * @return self
     */
    fun configure(configurer: QueryEnhancedRequest.Builder.() -> QueryEnhancedRequest.Builder): QueryBuilder<T> {
        delegate.configure { configurer(it) }
        return this
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    fun only(propertyPaths: Collection<String>): QueryBuilder<T> {
        delegate.only(propertyPaths)
        return this
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    fun only(vararg propertyPaths: String): QueryBuilder<T> {
        return only(listOf(*propertyPaths))
    }

}
