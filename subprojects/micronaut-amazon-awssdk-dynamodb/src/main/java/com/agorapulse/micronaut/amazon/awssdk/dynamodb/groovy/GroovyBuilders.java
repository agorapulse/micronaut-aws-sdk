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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.groovy;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ScanBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

public class GroovyBuilders extends Builders {

    /**
     * Creates query builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return query builder for given DynamoDB entity
     */
    public static <T> QueryBuilder<T> query(
        Class<T> type,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder<T>")
            Closure<QueryBuilder<T>> definition
    ) {
        return Builders.query(ConsumerWithDelegate.create(definition));
    }

    /**
     * Creates scan builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return scan builder for given DynamoDB entity
     */
    public static <T> ScanBuilder<T> scan(
        Class<T> type,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ScanBuilder<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.ScanBuilder<T>")
            Closure<ScanBuilder<T>> definition
    ) {
        return Builders.scan(ConsumerWithDelegate.create(definition));
    }

    /**
     * Creates update builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return update builder for given DynamoDB entity
     */
    public static <T> UpdateBuilder<T> update(
        Class<T> type,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder<T>")
            Closure<UpdateBuilder<T>> definition
    ) {
        return Builders.update(ConsumerWithDelegate.create(definition));
    }

}
