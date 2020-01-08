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
package com.agorapulse.micronaut.aws.dynamodb.builder

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBMetadata
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperTableModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.reactivex.Flowable

import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Default implementation of the scan builder.
 * @param <T> type of the item scanned
 */
@PackageScope
@CompileStatic
class DefaultScanBuilder<T> implements ScanBuilder<T> {

    DefaultScanBuilder(Class<T> type, DynamoDBScanExpression expression) {
        this.metadata = DynamoDBMetadata.create(type)
        this.expression = expression
    }

    DefaultScanBuilder<T> inconsistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            expression.withConsistentRead(false)
        }

        return this
    }

    DefaultScanBuilder<T> consistent(Builders.Read read) {
        if (read == Builders.Read.READ) {
            expression.withConsistentRead(true)
        }

        return this
    }

    DefaultScanBuilder<T> index(String name) {
        expression.withIndexName(name)
        return this
    }

    DefaultScanBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions) {
        filterCollectorsConsumers.add(conditions)
        return this
    }

    DefaultScanBuilder<T> filter(ConditionalOperator or) {
        expression.withConditionalOperator(or)
        return this
    }

    DefaultScanBuilder<T> page(int page) {
        expression.withLimit(page)
        return this
    }

    @Override
    ScanBuilder<T> limit(int max) {
        this.max = max
        return this
    }

    DefaultScanBuilder<T> offset(Object exclusiveHashStartKeyValue, Object exclusiveRangeStartKeyValue) {
        this.exclusiveHashStartKey = exclusiveHashStartKeyValue
        this.exclusiveRangeStartKey = exclusiveRangeStartKeyValue
        return this
    }

    @Override
    int count(IDynamoDBMapper mapper) {
        return mapper.count(metadata.itemClass, resolveExpression(mapper))
    }

    @Override
    Flowable<T> scan(IDynamoDBMapper mapper) {
        Flowable<T> results = FlowableQueryResultHelper.generate(metadata.itemClass, mapper, resolveExpression(mapper))
        if (max < Integer.MAX_VALUE) {
            return results.take(max)
        }
        return results
    }

    @Override
    DynamoDBScanExpression resolveExpression(IDynamoDBMapper mapper) {
        DynamoDBMapperTableModel<T> model = mapper.getTableModel(metadata.itemClass)

        applyConditions(model, filterCollectorsConsumers, expression.&withFilterConditionEntry)

        if (exclusiveHashStartKey != null || exclusiveRangeStartKey != null) {
            T exclusiveKey = model.createKey(exclusiveHashStartKey, exclusiveRangeStartKey)
            expression.withExclusiveStartKey(model.convertKey(exclusiveKey))
        }

        configurer.accept(expression)

        return expression
    }

    @Override
    ScanBuilder<T> configure(Consumer<DynamoDBScanExpression> configurer) {
        this.configurer = configurer
        return this
    }

    // for proper groovy evaluation of closure in the annotation
    @SuppressWarnings('UnusedMethodParameter')
    Object getProperty(String name) {
        throw new MissingPropertyException('No properties here!')
    }

    @Override
    ScanBuilder<T> only(Iterable<String> propertyPaths) {
        expression.projectionExpression = propertyPaths.join(',')
        return this
    }

    private void applyConditions(
        DynamoDBMapperTableModel<T> model,
        List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers,
        BiConsumer<String, Condition> addFilterConsumer
    ) {
        if (!filterCollectorsConsumers.empty) {
            RangeConditionCollector<T> filterCollector = new RangeConditionCollector<>(model)

            for (Consumer<RangeConditionCollector<T>> consumer : filterCollectorsConsumers) {
                consumer.accept(filterCollector)
            }

            filterCollector.conditions.forEach(addFilterConsumer)
        }
    }

    private final DynamoDBMetadata<T> metadata
    private final DynamoDBScanExpression expression
    private final List<Consumer<RangeConditionCollector<T>>> filterCollectorsConsumers = []

    private Consumer<DynamoDBScanExpression> configurer = { } as Consumer<DynamoDBScanExpression>
    private Object exclusiveHashStartKey
    private Object exclusiveRangeStartKey
    private int max = Integer.MAX_VALUE

}
