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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Converter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * An interface for updates which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedUpdate<T> {

    /**
     * Executes an update using provided mapper.
     * @param mapper DynamoDB mapper
     * @param client low level AWS SDK client
     * @return the return value which depends on the configuration of the update request
     */
    Object update(DynamoDbTable<T> mapper, DynamoDbClient client, Converter converter);

    /**
     * Resolves the current update into native update request using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current update resolved into native update request
     */
    UpdateItemRequest resolveExpression(DynamoDbTable<T>  mapper, Converter converter);

}
