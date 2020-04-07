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
import groovy.lang.MissingPropertyException;
import io.reactivex.Flowable;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.function.Consumer;

import static io.reactivex.Flowable.fromIterable;

/**
 * Default implementation of the query builder.
 * @param <T> type of the item queried
 */
class DefaultQueryBuilder<T> implements QueryBuilder<T> {

    DefaultQueryBuilder(QueryEnhancedRequest.Builder expression) {
        this.expression = expression;
    }

    @Override
    public DefaultQueryBuilder<T> sort(Builders.Sort sort) {
        if (sort == Builders.Sort.ASC) {
            expression.scanIndexForward(true);
        } else if (sort == Builders.Sort.DESC) {
            expression.scanIndexForward(false);
        }
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> inconsistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            expression.consistentRead(false);
        }

        return this;
    }

    @Override
    public DefaultQueryBuilder<T> consistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            expression.consistentRead(true);
        }

        return this;
    }

    @Override
    public DefaultQueryBuilder<T> index(String name) {
        this.index = name;
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> hash(Object hash) {
        return range(r -> {
            r.eq(r.getPartitionKey(), hash);
        });
    }

    @Override
    public DefaultQueryBuilder<T> range(Consumer<ConditionCollector<T>> conditions) {
        queryConditionals.add(conditions);
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> filter(Consumer<ConditionCollector<T>> conditions) {
        filterCollectorsConsumers.add(conditions);
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> page(int page) {
        expression.limit(page);
        return this;
    }

    @Override
    public QueryBuilder<T> limit(int max) {
        this.max = max;
        return this;
    }

    @Override
    public DefaultQueryBuilder<T> offset(AttributeValue exclusiveStartHashKeyValue, AttributeValue exclusiveRangeStartKey) {
        this.exclusiveHashStartKey = exclusiveStartHashKeyValue;
        this.exclusiveRangeStartKey = exclusiveRangeStartKey;
        return this;
    }

    @Override
    public int count(DynamoDbTable<T> mapper, Converter converter) {
        // TODO: use select
        return query(mapper, converter).count().blockingGet().intValue();
    }

    @Override
    public Flowable<T> query(DynamoDbTable<T> mapper, Converter converter) {
        QueryEnhancedRequest request = resolveRequest(mapper, converter);
        SdkIterable<Page<T>> iterable = this.index == null ? mapper.query(request) : mapper.index(index).query(request);
        Flowable<T> results = fromIterable(iterable).flatMap(p -> fromIterable(p.items()));;
        if (max < Integer.MAX_VALUE) {
            return results.take(max);
        }
        return results;
    }

    @Override
    public QueryEnhancedRequest resolveRequest(DynamoDbTable<T> mapper, Converter converter) {
        applyConditions(mapper, converter, queryConditionals, expression::queryConditional);
        String currentIndex = index == null ? TableMetadata.primaryIndexName() : index;
        applyConditions(mapper, converter, filterCollectorsConsumers, cond -> expression.filterExpression(cond.expression(mapper.tableSchema(), currentIndex)));

        if (exclusiveHashStartKey != null || exclusiveRangeStartKey != null) {
            Map<String, AttributeValue> exclusiveKey = new HashMap<>();
            final TableMetadata tableMetadata = mapper.tableSchema().tableMetadata();
            if (exclusiveHashStartKey != null) {
                exclusiveKey.put(tableMetadata.primaryPartitionKey(), exclusiveHashStartKey);
            }
            if (exclusiveRangeStartKey != null) {
                tableMetadata.primarySortKey().ifPresent(key -> {
                    exclusiveKey.put(key, exclusiveRangeStartKey);
                });
            }
            expression.exclusiveStartKey(exclusiveKey);
        }

        configurer.accept(expression);

        return expression.build();
    }

    @Override
    public QueryBuilder<T> only(Collection<String> propertyPaths) {
        expression.attributesToProject(propertyPaths);
        return this;
    }

    @Override
    public QueryBuilder<T> configure(Consumer<QueryEnhancedRequest.Builder> configurer) {
        this.configurer = configurer;
        return this;
    }

    @Override
    public String getIndex() {
        return index;
    }

    // for proper groovy evaluation of closure in the annotation
    @SuppressWarnings("UnusedMethodParameter")
    Object getProperty(String name) {
        // TODO: is this still required???
        throw new MissingPropertyException("No properties here!");
    }

    private void applyConditions(
        DynamoDbTable<T> model,
        Converter converter,
        List<Consumer<ConditionCollector<T>>> filterCollectorsConsumers,
        Consumer<QueryConditional> addFilterConsumer
    ) {
        if (!filterCollectorsConsumers.isEmpty()) {
            ConditionCollector<T> filterCollector = new ConditionCollector<>(model, converter);

            for (Consumer<ConditionCollector<T>> consumer : filterCollectorsConsumers) {
                consumer.accept(filterCollector);
            }

            addFilterConsumer.accept(filterCollector.getCondition());
        }
    }

    private final QueryEnhancedRequest.Builder expression;
    private final List<Consumer<ConditionCollector<T>>> filterCollectorsConsumers = new LinkedList<>();
    private final List<Consumer<ConditionCollector<T>>> queryConditionals = new LinkedList<>();

    private String index;
    private AttributeValue exclusiveHashStartKey;
    private AttributeValue exclusiveRangeStartKey;
    private int max = Integer.MAX_VALUE;
    private Consumer<QueryEnhancedRequest.Builder> configurer = b -> {};

}
