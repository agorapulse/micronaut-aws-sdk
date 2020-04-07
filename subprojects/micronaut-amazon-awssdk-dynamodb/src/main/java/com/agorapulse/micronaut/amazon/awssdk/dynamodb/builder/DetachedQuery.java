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

import io.reactivex.Flowable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * An interface for queries which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedQuery<T> {

    /**
     * Executes a query using provided mapper.
     * @param mapper DynamoDB mapper
     * @return flowable of entities found for the current query
     */
    Flowable<T> query(DynamoDbTable<T> mapper);

    /**
     * Counts entities satisfying given query using provided mapper.
     * @param mapper DynamoDB mapper
     * @return count of entities satisfying  for the current query
     */
    int count(DynamoDbTable<T> mapper);

    /**
     * Resolves the current query into native query expression using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current query resolved into native query expression
     */
    QueryEnhancedRequest resolveRequest(DynamoDbTable<T> mapper);

    /**
     * Returns the index for this query or null if the query is executed the primary index.
     * @return the index for this query or null if the query is executed the primary index
     */
    String getIndex();

}
