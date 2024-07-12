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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

import software.amazon.awssdk.services.dynamodb.model.ReturnValue
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

/**
 * Builder for DynamoDB updates.
 * @param <T> type of the DynamoDB entity
 * @param <R> type of the result returned from the update method
*/
class UpdateBuilder<T, R>(private val delegate: com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder<T, R>) {

    /**
     * Sets the partition key value of the updated entity.
     * @param key the partition key of the query
     * @return self
     */
    fun partitionKey(key: Any?): UpdateBuilder<T, R> {
        delegate.partitionKey(key)
        return this
    }

    /**
     * Sets the sort key value of the updated entity.
     * @param key the sort key of the updated entity
     * @return self
     */
    fun sortKey(key: Any?): UpdateBuilder<T, R> {
        delegate.sortKey(key)
        return this
    }

    /**
     * Add a difference to particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param delta the difference - usually a number or set of new items for set attributes
     * @return self
     */
    fun add(attributeName: String, delta: Any?): UpdateBuilder<T, R> {
        delegate.add(attributeName, delta)
        return this
    }

    /**
     * Sets a particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param value new value to be set
     * @return self
     */
    fun put(attributeName: String, value: Any?): UpdateBuilder<T, R> {
        delegate.put(attributeName, value)
        return this
    }

    /**
     * Deletes the value of the particular attribute of the entity.
     * @param attributeName name of the attribute
     * @return self
     */
    fun delete(attributeName: String): UpdateBuilder<T, R> {
        delegate.delete(attributeName)
        return this
    }

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
    </N> */
    fun <N> returns(returnValue: ReturnValue, mapper: T.() -> N): UpdateBuilder<T, N> {
        return UpdateBuilder(delegate.returns(returnValue) { mapper(it) })
    }

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @return self
     */
    fun returns(returnValue: ReturnValue): UpdateBuilder<T, T> {
        return returns(returnValue) { this }
    }

    /**
     * Declares that the update operation will not return any value.
     * @return self
     */
    fun returnNone(): UpdateBuilder<T, Unit> {
        return returns(ReturnValue.NONE) { Unit }
    }

    /**
     * Declares that the update operation will return all previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
    </N> */
    fun <N> returnAllOld(mapper: T.() ->  N): UpdateBuilder<T, N> {
        return returns(ReturnValue.ALL_OLD, mapper)
    }

    /**
     * Declares that the update operation will only return updated previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
    </N> */
    fun <N> returnUpdatedOld(mapper: T.() -> N): UpdateBuilder<T, N> {
        return returns(ReturnValue.UPDATED_OLD, mapper)
    }

    /**
     * Declares that the update operation will return all new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
    </N> */
    fun <N> returnAllNew(mapper: T.() -> N): UpdateBuilder<T, N> {
        return returns(ReturnValue.ALL_NEW, mapper)
    }

    /**
     * Declares that the update operation will only return updated new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
    </N> */
    fun <N> returnUpdatedNew(mapper:  T.() -> N): UpdateBuilder<T, N> {
        return returns(ReturnValue.UPDATED_NEW, mapper)
    }

    /**
     * Configures the native update request.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native update request
     * @return self
     */
    fun configure(configurer: UpdateItemRequest.Builder.() -> UpdateItemRequest.Builder): UpdateBuilder<T, R> {
        delegate.configure { configurer(it) }
        return this
    }

    fun toJavaBuilder(): com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder<T, R> {
        return delegate
    }
}
