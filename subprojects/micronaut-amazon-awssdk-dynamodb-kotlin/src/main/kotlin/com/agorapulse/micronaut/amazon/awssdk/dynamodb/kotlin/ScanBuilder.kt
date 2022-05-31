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
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest

/**
 * Builder for DynamoDB scans.
 * @param <T> type of the DynamoDB entity
 */
class ScanBuilder<T>(private val delegate: com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ScanBuilder<T>) {

    /**
     * Demand consistent reads.
     * @param read the read keyword
     * @return self
     */
    fun consistent(read: Builders.Read): ScanBuilder<T> {
        delegate.consistent(read)
        return this;
    }

    /**
     * Demand inconsistent reads.
     * @param read the read keyword
     * @return self
     */
    fun inconsistent(read: Builders.Read): ScanBuilder<T> {
        delegate.inconsistent(read)
        return this;
    }

    /**
     * Select the index on which this scan will be executed.
     * @param name the name of the index to be used
     * @return self
     */
    fun index(name: String): ScanBuilder<T> {
        delegate.index(name)
        return this
    }

    /**
     * One or more filter conditions.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    fun filter(conditions: FilterConditionCollector<T>.() -> FilterConditionCollector<T>): ScanBuilder<T> {
        delegate.filter { conditions(FilterConditionCollector(it)) }
        return this;
    }

    /**
     * Sets the desired pagination of the scans.
     *
     * This only sets the optimal pagination of the scans and does not limit the number of items returned.
     *
     * Use `[io.reactivex.Flowable.take]` to limit the number results returned from the scan.
     *
     * @param page number of entities loaded by one scan request (not a number of total entities returned)
     * @return self
     */
    fun page(page: Int): ScanBuilder<T> {
        delegate.page(page)
        return this
    }

    /**
     * Sets the maximum number of items to be returned from the queries.
     *
     * This is a shortcut for calling `[io.reactivex.Flowable.take]` on the result Flowable.
     *
     * @param max the maximum number of items returned
     * @return self
     */
    fun limit(max: Int): ScanBuilder<T> {
        delegate.limit(max)
        return this
    }

    /**
     * Sets the scan offset by defining the exclusive start value.
     * @param lastEvaluatedKey exclusive start value
     * @return self
     */
    fun lastEvaluatedKey(lastEvaluatedKey: Any): ScanBuilder<T> {
        delegate.lastEvaluatedKey(lastEvaluatedKey)
        return this
    }

    /**
     * Configures the native scan expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native scan expression
     * @return self
     */
    fun configure(configurer: ScanEnhancedRequest.Builder.() -> ScanEnhancedRequest.Builder): ScanBuilder<T> {
        delegate.configure { configurer(it) }
        return this
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    fun only(propertyPaths: Collection<String>): ScanBuilder<T> {
        delegate.only(propertyPaths)
        return this
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    fun only(vararg propertyPaths: String): ScanBuilder<T> {
        return only(listOf(*propertyPaths))
    }

}
