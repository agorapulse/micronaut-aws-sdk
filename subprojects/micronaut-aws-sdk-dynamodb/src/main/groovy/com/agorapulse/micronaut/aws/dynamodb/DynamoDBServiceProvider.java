/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider of {@link DynamoDBService} for particular DynamoDB entities.
 */
@Singleton
@Requires(classes = IDynamoDBMapper.class)
public class DynamoDBServiceProvider {

    private final ConcurrentHashMap<Class, DynamoDBService> serviceCache = new ConcurrentHashMap<>();
    private final AmazonDynamoDB client;
    private final IDynamoDBMapper mapper;

    public DynamoDBServiceProvider(AmazonDynamoDB client, IDynamoDBMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Provides {@link DynamoDBService} for given type.
     *
     * @param type DynamoDB entity type.
     * @param <T> the type of the DynamoDB entity
     * @return {@link DynamoDBService} for given type
     */
    public <T> DynamoDBService<T> findOrCreate(Class<T> type) {
        return serviceCache.computeIfAbsent(type, t -> new DefaultDynamoDBService<T>(client, mapper, type));
    }

}
