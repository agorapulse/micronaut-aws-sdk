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
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional.QueryConditionalFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Default implementation of the query builder.
 * @param <T> type of the item queried
 */
class DefaultQueryBuilder<T> implements QueryBuilder<T> {

    DefaultQueryBuilder(QueryEnhancedRequest.Builder expression) {
        this.__expression = expression;
    }

    @Override
    public DefaultQueryBuilder<T> order(Builders.Sort sort) {
        if (sort == Builders.Sort.ASC) {
            __expression.scanIndexForward(true);
        } else if (sort == Builders.Sort.DESC) {
            __expression.scanIndexForward(false);
        }
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> inconsistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            __expression.consistentRead(false);
        }

        return this;
    }

    @Override
    public DefaultQueryBuilder<T> consistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            __expression.consistentRead(true);
        }

        return this;
    }

    @Override
    public DefaultQueryBuilder<T> index(String name) {
        this.__index = name;
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> partitionKey(Object hash) {
        this.__hash = hash;
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> sortKey(Consumer<KeyConditionCollector<T>> conditions) {
        __queryConditionals.add(conditions);
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> filter(Consumer<FilterConditionCollector<T>> conditions) {
        __filterCollectorsConsumers.add(conditions);
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> page(int page) {
        __expression.limit(page);
        return this;
    }

    @Override
    public QueryBuilder<T> limit(int max) {
        this.__max = max;
        return this;
    }

    @Override
    public QueryBuilder<T> lastEvaluatedKey(Object lastEvaluatedKey) {
        this.__lastEvaluatedKey = lastEvaluatedKey;
        return this;
    }

    @Override
    public int count(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        // TODO: use select
        return Flux.from(query(mapper, attributeConversionHelper)).count().blockOptional().map(Long::intValue).orElse(0);
    }

    @Override
    public Mono<Long> count(DynamoDbAsyncTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        return query(mapper, attributeConversionHelper).count();
    }

    @Override
    public Flux<T> query(DynamoDbTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        QueryEnhancedRequest request = resolveRequest(mapper, attributeConversionHelper);
        SdkIterable<Page<T>> iterable = this.__index == null ? mapper.query(request) : mapper.index(__index).query(request);
        Flux<T> results = Flux.fromIterable(iterable).flatMap(p -> Flux.fromIterable(p.items()));
        if (__max < Integer.MAX_VALUE) {
            return results.take(__max);
        }
        return results;
    }

    @Override
    public QueryEnhancedRequest resolveRequest(MappedTableResource<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        applyRangeConditions(mapper, attributeConversionHelper, __expression::queryConditional);
        String currentIndex = __index == null ? TableMetadata.primaryIndexName() : __index;
        applyFilterConditions(mapper, attributeConversionHelper, cond -> __expression.filterExpression(cond.expression(mapper.tableSchema(), currentIndex)));
        applyLastEvaluatedKey(__expression, mapper);
        __configurer.accept(__expression);

        return __expression.build();
    }

    @Override
    public Flux<T> query(DynamoDbAsyncTable<T> mapper, AttributeConversionHelper attributeConversionHelper) {
        QueryEnhancedRequest request = resolveRequest(mapper, attributeConversionHelper);
        SdkPublisher<Page<T>> iterable = this.__index == null ? mapper.query(request) : mapper.index(__index).query(request);
        Flux<T> results = Flux.from(iterable).flatMap(p -> Flux.fromIterable(p.items()));
        if (__max < Integer.MAX_VALUE) {
            return results.take(__max);
        }
        return results;
    }

    @Override
    public QueryBuilder<T> only(Collection<String> propertyPaths) {
        __expression.attributesToProject(propertyPaths);
        return this;
    }

    @Override
    public QueryBuilder<T> configure(Consumer<QueryEnhancedRequest.Builder> configurer) {
        this.__configurer = configurer;
        return this;
    }

    private void applyRangeConditions(
        MappedTableResource<T> model,
        AttributeConversionHelper attributeConversionHelper,
        Consumer<QueryConditional> addFilterConsumer
    ) {

        DefaultKeyConditionCollector<T> rangeCollector = new DefaultKeyConditionCollector<>(model, attributeConversionHelper, __index);

        if (!__queryConditionals.isEmpty()) {

            for (Consumer<KeyConditionCollector<T>> consumer : __queryConditionals) {
                consumer.accept(rangeCollector);
            }

        }

        String partitionKey = model.tableSchema().tableMetadata().indexPartitionKey(__index);
        QueryConditional hashCondition = QueryConditionalFactory.equalTo(partitionKey, attributeConversionHelper.convert(model, partitionKey, __hash));

        addFilterConsumer.accept(QueryConditionalFactory.and(hashCondition, rangeCollector.getCondition()));
    }

    private void applyFilterConditions(
        MappedTableResource<T> model,
        AttributeConversionHelper attributeConversionHelper,
        Consumer<QueryConditional> addFilterConsumer
    ) {
        if (!__filterCollectorsConsumers.isEmpty()) {
            DefaultFilterConditionCollector<T> filterCollector = new DefaultFilterConditionCollector<>(model, attributeConversionHelper);

            for (Consumer<FilterConditionCollector<T>> consumer : __filterCollectorsConsumers) {
                consumer.accept(filterCollector);
            }

            addFilterConsumer.accept(filterCollector.getCondition());
        }
    }

    private void applyLastEvaluatedKey(QueryEnhancedRequest.Builder exp, MappedTableResource<T> mapper) {
        if (__lastEvaluatedKey == null) {
            return;
        }

        Map<String, AttributeValue> key;
        TableSchema<T> schema = mapper.tableSchema();

        if (__lastEvaluatedKey instanceof Map) {
            key = (Map<String, AttributeValue>) __lastEvaluatedKey;
        } else {
            key = schema.itemToMap((T) __lastEvaluatedKey, true);
        }

        Set<String> indexKeys = new HashSet<>(schema.tableMetadata().primaryKeys());

        if (__index != null) {
            indexKeys.addAll(schema.tableMetadata().indexKeys(__index));
        }

        key = key.entrySet().stream().filter(e -> indexKeys.contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        exp.exclusiveStartKey(key);
    }

    // fields are prefixed with "__" to allow groovy evaluation of the arguments
    // otherwise if the argument has the same name (such as max) it will be ignored and field value will be used
    private final QueryEnhancedRequest.Builder __expression;
    private final List<Consumer<FilterConditionCollector<T>>> __filterCollectorsConsumers = new LinkedList<>();
    private final List<Consumer<KeyConditionCollector<T>>> __queryConditionals = new LinkedList<>();

    private String __index = TableMetadata.primaryIndexName();
    private Object __hash;
    private Object __lastEvaluatedKey;
    private int __max = Integer.MAX_VALUE;
    private Consumer<QueryEnhancedRequest.Builder> __configurer = b -> {};

}
