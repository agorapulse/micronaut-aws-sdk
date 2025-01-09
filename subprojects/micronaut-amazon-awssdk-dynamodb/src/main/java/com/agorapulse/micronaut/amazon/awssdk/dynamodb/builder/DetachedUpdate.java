/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * An interface for updates which can be executed using supplied mapper.
 * @param <T> type of the DynamoDB entity
 */
public interface DetachedUpdate<T, R> {

    /**
     * Executes an update using provided mapper.
     * @param mapper DynamoDB mapper
     * @param client low level AWS SDK client
     * @param publisher application event publisher
     * @return the return value which depends on the configuration of the update request
     */
    R update(DynamoDbTable<T> mapper, DynamoDbClient client, AttributeConversionHelper attributeConversionHelper, ApplicationEventPublisher publisher);

    /**
     * Resolves the current update into native update request using provided mapper.
     * @param mapper DynamoDB mapper
     * @return the current update resolved into native update request
     */
    UpdateItemRequest resolveRequest(MappedTableResource<T> mapper, AttributeConversionHelper attributeConversionHelper);

    /**
     * Executes an update using provided mapper.
     * @param table DynamoDB mapper
     * @param client low level AWS SDK client
     * @param publisher application event publisher
     * @return the return value which depends on the configuration of the update request
     */
    Publisher<R> update(DynamoDbAsyncTable<T> table, DynamoDbAsyncClient client, AttributeConversionHelper attributeConversionHelper, ApplicationEventPublisher publisher);
}
