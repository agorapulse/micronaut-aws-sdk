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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider of {@link AsyncDynamoDbService} for particular DynamoDB entities.
 */
@Singleton
public class DefaultAsyncDynamoDBServiceProvider implements AsyncDynamoDBServiceProvider {

    private final ConcurrentHashMap<String, AsyncDynamoDbService<?>> serviceCache = new ConcurrentHashMap<>();
    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final DynamoDbAsyncClient client;
    private final AttributeConversionHelper attributeConversionHelper;
    private final ApplicationEventPublisher publisher;
    private final TableSchemaCreator tableSchemaCreator;
    private final boolean createTables;

    public DefaultAsyncDynamoDBServiceProvider(
        DynamoDbEnhancedAsyncClient enhancedClient,
        DynamoDbAsyncClient client,
        AttributeConversionHelper attributeConversionHelper,
        ApplicationEventPublisher publisher,
        TableSchemaCreator tableSchemaCreator,
        @Value("${aws.dynamodb.create-tables:false}") boolean createTables
    ) {
        this.enhancedClient = enhancedClient;
        this.client = client;
        this.attributeConversionHelper = attributeConversionHelper;
        this.publisher = publisher;
        this.tableSchemaCreator = tableSchemaCreator;
        this.createTables = createTables;
    }

    /**
     * Provides {@link DynamoDbService} for given type.
     *
     * @param tableName name of the table
     * @param type      DynamoDB entity type
     * @param <T>       the type of the DynamoDB entity
     * @return {@link DynamoDbService} for given type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncDynamoDbService<T> findOrCreate(String tableName, Class<T> type) {
        return (AsyncDynamoDbService<T>) serviceCache.computeIfAbsent(tableName, t -> {
                DynamoDbAsyncTable<T> table = enhancedClient.table(tableName, tableSchemaCreator.create(type));

                DefaultAsyncDynamoDbService<T> newService = new DefaultAsyncDynamoDbService<>(
                    type,
                    enhancedClient,
                    client,
                    attributeConversionHelper,
                    publisher,
                    table
                );

                if (!createTables) {
                    return newService;
                }

                Mono.fromFuture(table.describeTable())
                    .map(Objects::nonNull)
                    .onErrorResume(
                        ResourceNotFoundException.class,
                        e -> Mono.from(newService.createTable())
                    )
                    .block(Duration.ofSeconds(30));

                return newService;
            }
        );
    }

}
