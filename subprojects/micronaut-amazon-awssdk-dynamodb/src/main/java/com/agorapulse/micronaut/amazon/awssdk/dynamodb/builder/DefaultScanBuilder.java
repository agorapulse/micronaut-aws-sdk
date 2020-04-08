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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.AttributeConversionHelper;
import io.reactivex.Flowable;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.function.Consumer;

import static io.reactivex.Flowable.fromIterable;

/**
 * Default implementation of the query builder.
 * @param <T> type of the item queried
 */
class DefaultScanBuilder<T> implements ScanBuilder<T> {

    DefaultScanBuilder(ScanEnhancedRequest.Builder expression) {
        this.__expression = expression;
    }

    @Override
    public DefaultScanBuilder<T> inconsistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            __expression.consistentRead(false);
        }

        return this;
    }

    @Override
    public DefaultScanBuilder<T> consistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            __expression.consistentRead(true);
        }

        return this;
    }

    @Override
    public DefaultScanBuilder<T> index(String name) {
        this.__index = name;
        return this;
    }

    @Override
    public DefaultScanBuilder<T> filter(Consumer<ConditionCollector<T>> conditions) {
        __filterCollectorsConsumers.add(conditions);
        return this;
    }

    @Override
    public DefaultScanBuilder<T> page(int page) {
        __expression.limit(page);
        return this;
    }

    @Override
    public ScanBuilder<T> limit(int max) {
        this.__max = max;
        return this;
    }

    @Override
    public DefaultScanBuilder<T> offset(AttributeValue exclusiveStartHashKeyValue, AttributeValue exclusiveRangeStartKey) {
        this.__exclusiveHashStartKey = exclusiveStartHashKeyValue;
        this.__exclusiveRangeStartKey = exclusiveRangeStartKey;
        return this;
    }

    @Override
    public int count(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        // TODO: use select
        return scan(mapper, attributeConversionHelper).count().blockingGet().intValue();
    }

    @Override
    public Flowable<T> scan(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        ScanEnhancedRequest request = resolveRequest(mapper, attributeConversionHelper);
        SdkIterable<Page<T>> iterable = this.__index == null ? mapper.scan(request) : mapper.index(__index).scan(request);
        Flowable<T> results = fromIterable(iterable).flatMap(p -> fromIterable(p.items()));
        if (__max < Integer.MAX_VALUE) {
            return results.take(__max);
        }
        return results;
    }

    @Override
    public ScanEnhancedRequest resolveRequest(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        String currentIndex = __index == null ? TableMetadata.primaryIndexName() : __index;
        applyConditions(mapper, attributeConversionHelper, __filterCollectorsConsumers, cond -> __expression.filterExpression(cond.expression(mapper.tableSchema(), currentIndex)));

        if (__exclusiveHashStartKey != null || __exclusiveRangeStartKey != null) {
            Map<String, AttributeValue> exclusiveKey = new HashMap<>();
            final TableMetadata tableMetadata = mapper.tableSchema().tableMetadata();
            if (__exclusiveHashStartKey != null) {
                exclusiveKey.put(tableMetadata.primaryPartitionKey(), __exclusiveHashStartKey);
            }
            if (__exclusiveRangeStartKey != null) {
                tableMetadata.primarySortKey().ifPresent(key -> {
                    exclusiveKey.put(key, __exclusiveRangeStartKey);
                });
            }
            __expression.exclusiveStartKey(exclusiveKey);
        }

        __configurer.accept(__expression);

        return __expression.build();
    }

    @Override
    public ScanBuilder<T> only(Collection<String> propertyPaths) {
        __expression.attributesToProject(propertyPaths);
        return this;
    }

    @Override
    public ScanBuilder<T> configure(Consumer<ScanEnhancedRequest.Builder> configurer) {
        this.__configurer = configurer;
        return this;
    }

    private void applyConditions(
        DynamoDbTable<T> table,
        AttributeConversionHelper attributeConversionHelper,
        List<Consumer<ConditionCollector<T>>> filterCollectorsConsumers,
        Consumer<QueryConditional> addFilterConsumer
    ) {
        if (!filterCollectorsConsumers.isEmpty()) {
            ConditionCollector<T> filterCollector = new ConditionCollector<>(table, attributeConversionHelper);

            for (Consumer<ConditionCollector<T>> consumer : filterCollectorsConsumers) {
                consumer.accept(filterCollector);
            }

            addFilterConsumer.accept(filterCollector.getCondition());
        }
    }

    // fields are prefixed with "__" to allow groovy evaluation of the arguments
    // otherwise if the argument has the same name (such as max) it will be ignored and field value will be used
    private final ScanEnhancedRequest.Builder __expression;
    private final List<Consumer<ConditionCollector<T>>> __filterCollectorsConsumers = new LinkedList<>();

    private String __index;
    private AttributeValue __exclusiveHashStartKey;
    private AttributeValue __exclusiveRangeStartKey;
    private int __max = Integer.MAX_VALUE;
    private Consumer<ScanEnhancedRequest.Builder> __configurer = b -> {};

}
