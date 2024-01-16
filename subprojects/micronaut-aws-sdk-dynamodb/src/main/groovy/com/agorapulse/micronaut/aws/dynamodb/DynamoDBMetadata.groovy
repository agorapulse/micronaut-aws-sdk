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
package com.agorapulse.micronaut.aws.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.*
import groovy.transform.CompileDynamic

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Collector of DynamoDB metadata.
 * @param <T> type of the DynamoDB entity
 */
@CompileDynamic
@SuppressWarnings('NoWildcardImports')
class DynamoDBMetadata<T> {

    private static final Map<Class, DynamoDBMetadata> CACHE = new ConcurrentHashMap<>()

    /**
     * Crates metadata for particular type or populates it from cache if already resolved.
     * @param type
     * @return
     */
    static <T> DynamoDBMetadata<T> create(Class<T> type) {
        return CACHE.computeIfAbsent(type) {
            new DynamoDBMetadata<T>(it)
        }
    }

    final String hashKeyName
    final Class hashKeyClass
    final Class<T> itemClass
    final DynamoDBTable mainTable
    final String rangeKeyName
    final Class rangeKeyClass
    final List<String> secondaryIndexes

    private DynamoDBMetadata(Class<T> itemClass) {
        this.itemClass = itemClass
        this.mainTable = (DynamoDBTable) itemClass.getAnnotation(DynamoDBTable)

        if (!mainTable) {
            throw new IllegalArgumentException("Missing @DynamoDBTable annotation on class: ${itemClass}")
        }

        List<String> secondaryIndexes = []

        // Annotations on fields
        for (Field field in itemClass.declaredFields) {
            // Get hash key
            if (field.getAnnotation(DynamoDBHashKey)) {
                hashKeyName = field.name
                hashKeyClass = field.type
            }
            // Get range key
            if (field.getAnnotation(DynamoDBRangeKey)) {
                rangeKeyName = field.name
                rangeKeyClass = field.type
            }
            // Get secondary indexes
            DynamoDBIndexRangeKey indexRangeKeyAnnotation = field.getAnnotation(DynamoDBIndexRangeKey)
            if (indexRangeKeyAnnotation) {
                secondaryIndexes.add(indexRangeKeyAnnotation.localSecondaryIndexName())
            }
        }

        // Annotations on methods
        for (Method method in itemClass.declaredMethods) {
            if (method.name.startsWith('get') || method.name.startsWith('is')) {
                // Get hash key
                if (method.getAnnotation(DynamoDBHashKey)) {
                    hashKeyName = ReflectionUtils.getFieldNameByGetter(method, true)
                    hashKeyClass = itemClass.getDeclaredField(hashKeyName).type
                }
                // Get range key
                if (method.getAnnotation(DynamoDBRangeKey)) {
                    rangeKeyName = ReflectionUtils.getFieldNameByGetter(method, true)
                    rangeKeyClass = itemClass.getDeclaredField(rangeKeyName).type
                }
                // Get secondary indexes
                DynamoDBIndexRangeKey indexRangeKeyAnnotation = method.getAnnotation(DynamoDBIndexRangeKey)
                if (indexRangeKeyAnnotation) {
                    secondaryIndexes.add(indexRangeKeyAnnotation.localSecondaryIndexName())
                }
            }
        }

        if (!hashKeyName || !hashKeyClass) {
            throw new IllegalArgumentException("Missing hashkey annotations on class: ${itemClass}")
        }

        this.secondaryIndexes = secondaryIndexes.asImmutable()
    }

}
