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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder for DynamoDB updates.
 * @param <T> type of the DynamoDB entity
 * @param <R> type of the result returned from the update method
 */
public interface UpdateBuilder<T, R> extends DetachedUpdate<T, R> {

    /**
     * Sets the partition key value of the updated entity.
     * @param key the partition key of the query
     * @return self
     */
    UpdateBuilder<T, R> partitionKey(Object key);

    /**
     * Sets the partition key value of the updated entity.
     * @param key the partition key of the query
     * @return self
     * @deprecated use {@link #partitionKey(Object)} instead
     */
    @Deprecated
    default UpdateBuilder<T, R> hash(Object key) {
        return partitionKey(key);
    }

    /**
     * Sets the sort key value of the updated entity.
     * @param key the sort key of the updated entity
     * @return self
     */
    UpdateBuilder<T, R> sortKey(Object key);

    /**
     * Sets the sort key value of the updated entity.
     * @param key the sort key of the updated entity
     * @return self
     * @deprecated use {@link #sortKey(Object)} instead
     */
    @Deprecated
    default UpdateBuilder<T, R> range(Object key) {
        return sortKey(key);
    }

    /**
     * Add a difference to particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param delta the difference - usually a number or set of new items for set attributes
     * @return self
     */
    UpdateBuilder<T, R> add(String attributeName, Object delta);

    /**
     * Sets a particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param value new value to be set
     * @return self
     */
    UpdateBuilder<T, R> put(String attributeName, Object value);

    /**
     * Deletes the value of the particular attribute of the entity.
     * @param attributeName name of the attribute
     * @return self
     */
    UpdateBuilder<T, R> delete(String attributeName);

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
     */
    <N> UpdateBuilder<T, N> returns(ReturnValue returnValue, Function<T, N> mapper);

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @return self
     */
    default UpdateBuilder<T, T> returns(ReturnValue returnValue) {
        return returns(returnValue, Function.identity());
    }

    /**
     * Declares that the update operation will not return any value.
     * @return self
     */
    default UpdateBuilder<T, Void> returnNone() {
        return returns(ReturnValue.NONE, i -> null);
    }

    /**
     * Declares that the update operation will return all previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
     */
    default <N> UpdateBuilder<T, N> returnAllOld(Function<T, N> mapper) {
        return returns(ReturnValue.ALL_OLD, mapper);
    }

    /**
     * Declares that the update operation will only return updated previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
     */
    default <N> UpdateBuilder<T, N> returnUpdatedOld(Function<T, N> mapper) {
        return returns(ReturnValue.UPDATED_OLD, mapper);
    }

    /**
     * Declares that the update operation will return all new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
     */
    default <N> UpdateBuilder<T, N> returnAllNew(Function<T, N> mapper) {
        return returns(ReturnValue.ALL_NEW, mapper);
    }

    /**
     * Declares that the update operation will only return updated new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     * @param <N> new return type of the update call
     */
    default <N> UpdateBuilder<T, N> returnUpdatedNew(Function<T, N> mapper) {
        return returns(ReturnValue.UPDATED_NEW, mapper);
    }

    /**
     * Configures the native update request.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native update request
     * @return self
     */
    UpdateBuilder<T, R> configure(Consumer<UpdateItemRequest.Builder> configurer);

}
