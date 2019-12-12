/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;
import space.jasan.support.groovy.closure.FunctionWithDelegate;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder for DynamoDB updates.
 * @param <T> type of the DynamoDB entity
 */
public interface UpdateBuilder<T> extends DetachedUpdate<T> {

    /**
     * Sets the hash key value of the updated entity.
     * @param key the hash key of the query or an instance of the object with the hash key set
     * @return self
     */
    UpdateBuilder<T> hash(Object key);

    /**
     * Sets the range key value of the updated entity.
     * @param range the range key of the updated entity
     * @return self
     */
    UpdateBuilder<T> range(Object range);

    /**
     * Add a difference to particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param delta the difference - usually a number or set of new items for set attributes
     * @return self
     */
    UpdateBuilder<T> add(String attributeName, Object delta);

    /**
     * Sets a particular attribute of the entity.
     * @param attributeName name of the attribute
     * @param value new value to be set
     * @return self
     */
    UpdateBuilder<T> put(String attributeName, Object value);

    /**
     * Deletes the value of the particular attribute of the entity.
     * @param attributeName name of the attribute
     * @return self
     */
    UpdateBuilder<T> delete(String attributeName);

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    UpdateBuilder<T> returns(ReturnValue returnValue, Function<T, ?> mapper);

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none, old or new, all or updated attributes should be returned
     * @return self
     */
    default UpdateBuilder<T> returns(ReturnValue returnValue) {
        return returns(returnValue, Function.identity());
    }

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none (default), old or new, all or updated attributes should be returned
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returns(
        ReturnValue returnValue,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(returnValue, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will not return any value.
     * @return self
     */
    default UpdateBuilder<T> returnNone() {
        return returns(ReturnValue.NONE, Function.identity());
    }

    /**
     * Declares that the update operation will return all previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnAllOld(Function<T, ?> mapper) {
        return returns(ReturnValue.ALL_OLD, mapper);
    }

    /**
     * Declares that the update operation will return all previous values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnAllOld(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.ALL_OLD, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will only return updated previous values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnUpdatedOld(Function<T, ?> mapper) {
        return returns(ReturnValue.UPDATED_OLD, mapper);
    }

    /**
     * Declares that the update operation will only return updated previous values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnUpdatedOld(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.UPDATED_OLD, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will return all new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnAllNew(Function<T, ?> mapper) {
        return returns(ReturnValue.ALL_NEW, mapper);
    }

    /**
     * Declares that the update operation will return all new values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnAllNew(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.ALL_NEW, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will only return updated new values.
     * @param mapper function to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnUpdatedNew(Function<T, ?> mapper) {
        return returns(ReturnValue.UPDATED_NEW, mapper);
    }

    /**
     * Declares that the update operation will only return updated new values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    default UpdateBuilder<T> returnUpdatedNew(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return returns(ReturnValue.UPDATED_NEW, FunctionWithDelegate.create(mapper));
    }

    /**
     * Configures the native update request.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native update request
     * @return self
     */
    UpdateBuilder<T> configure(Consumer<UpdateItemRequest> configurer);

    /**
     * Configures the native update request.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native update request
     * @return self
     */
    default UpdateBuilder<T> configure(
        @DelegatesTo(type = "com.amazonaws.services.dynamodbv2.model.UpdateItemRequest", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.amazonaws.services.dynamodbv2.model.UpdateItemRequest")
            Closure<Object> configurer
    ) {
        return configure(ConsumerWithDelegate.create(configurer));
    }

}
