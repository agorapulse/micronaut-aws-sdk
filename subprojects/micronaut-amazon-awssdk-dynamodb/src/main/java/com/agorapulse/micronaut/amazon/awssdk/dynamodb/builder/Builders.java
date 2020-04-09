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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.function.Consumer;

/**
 * Utility class for building queries, updates and scan.
 *
 * This class is designed to be statically star-imported in the service interface.
 */
public class Builders {

    private static final int DEFAULT_QUERY_LIMIT = 20;

    enum Sort {
        ASC, DESC
    }

    enum Read {
        READ
    }

    //CHECKSTYLE:OFF: ConstantName - DSL keywords
    public static final Sort asc = Sort.ASC;
    public static final Sort desc = Sort.DESC;
    public static final Read read = Read.READ;

    public static final ReturnValue none = ReturnValue.NONE;
    public static final ReturnValue allOld = ReturnValue.ALL_OLD;
    public static final ReturnValue updatedOld = ReturnValue.UPDATED_OLD;
    public static final ReturnValue allNew = ReturnValue.ALL_NEW;
    public static final ReturnValue updatedNew = ReturnValue.UPDATED_NEW;
    //CHECKSTYLE.ON: ConstantName

    /**
     * Creates query builder for given DynamoDB entity.
     *
     * @param <T> type of DynamoDB entity
     * @return query builder for given DynamoDB entity
     */
    public static <T> QueryBuilder<T> query(Class<T> type) {
        return new DefaultQueryBuilder<>(QueryEnhancedRequest.builder().limit(DEFAULT_QUERY_LIMIT));
    }

    /**
     * Creates query builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return query builder for given DynamoDB entity
     */
    public static <T> QueryBuilder<T> query(Class<T> type, Consumer<QueryBuilder<T>> definition) {
        QueryBuilder<T> builder = query(type);
        definition.accept(builder);
        return builder;
    }

    /**
     * Creates scan builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param <T> type of DynamoDB entity
     * @return scan builder for given DynamoDB entity
     */
    public static <T> ScanBuilder<T> scan(Class<T> type) {
        return new DefaultScanBuilder<T>(ScanEnhancedRequest.builder().limit(DEFAULT_QUERY_LIMIT));
    }

    /**
     * Creates scan builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return scan builder for given DynamoDB entity
     */
    public static <T> ScanBuilder<T> scan(Class<T> type, Consumer<ScanBuilder<T>> definition) {
        ScanBuilder<T> builder = scan(type);
        definition.accept(builder);
        return builder;
    }

    /**
     * Creates update builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param <T> type of DynamoDB entity
     * @return update builder for given DynamoDB entity
     */
    public static <T> UpdateBuilder<T> update(Class<T> type) {
        return new DefaultUpdateBuilder<>();
    }

    /**
     * Creates update builder for given DynamoDB entity.
     *
     * @param type DynamoDB entity type
     * @param definition definition of the query
     * @param <T> type of DynamoDB entity
     * @return update builder for given DynamoDB entity
     */
    public static <T> UpdateBuilder<T> update(Class<T> type, Consumer<UpdateBuilder<T>> definition) {
        UpdateBuilder<T> builder = update(type);
        definition.accept(builder);
        return builder;
    }

}
