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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.util;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryBuilder;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class QueryArguments {

    private static final String PARTITION = "partition";
    private static final String SORT = "sort";
    private static final String HASH = "hash";
    private static final String RANGE = "range";

    private final Map<String, FilterArgument> filters = new LinkedHashMap<>();

    private Argument<?> partitionKey;
    private FilterArgument sortKey;
    private String index;
    private boolean consistent;
    private boolean descending;

    public static QueryArguments create(MethodInvocationContext<Object, Object> context, TableMetadata tableMetadata) {
        QueryArguments queryArguments = new QueryArguments();

        queryArguments.index = context.getTargetMethod().isAnnotationPresent(Index.class) ? context.getTargetMethod().getAnnotation(Index.class).value() : null;
        queryArguments.consistent = context.getTargetMethod().isAnnotationPresent(Consistent.class) && context.getTargetMethod().getAnnotation(Consistent.class).value();
        queryArguments.descending = context.getTargetMethod().isAnnotationPresent(Descending.class) && context.getTargetMethod().getAnnotation(Descending.class).value();


        Argument<?>[] arguments = context.getArguments();
        for (Argument<?> argument : arguments) {
            if (
                argument.isAnnotationPresent(SortKey.class)
                    || argument.isAnnotationPresent(RangeKey.class)
                    || argument.getName().toLowerCase().contains(SORT)
                    || argument.getName().toLowerCase().contains(RANGE)
                    || argument.getName().equals(tableMetadata.primarySortKey().orElse(SORT))
            ) {
                if (queryArguments.sortKey == null) {
                    queryArguments.sortKey = new FilterArgument();
                }
                queryArguments.sortKey.fill(argument);
            } else if (
                argument.isAnnotationPresent(PartitionKey.class)
                    || argument.isAnnotationPresent(HashKey.class)
                    || argument.getName().toLowerCase().contains(PARTITION)
                    || argument.getName().toLowerCase().contains(HASH)
                    || argument.getName().equals(tableMetadata.primaryPartitionKey())
            ) {
                queryArguments.partitionKey = argument;
            } else {
                String name = FilterArgument.getArgumentName(argument);
                queryArguments.filters.computeIfAbsent(name, argName -> new FilterArgument()).fill(argument);
            }
        }

        if (!queryArguments.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument annotated with @PartitionKey or with called 'partition'");
        }

        return queryArguments;
    }

    @SuppressWarnings("unchecked")
    public static <T> Publisher<T> toPublisher(ConversionService conversionService, Class<T> type, Argument<?> itemArgument, Map<String, MutableArgumentValue<?>> params) {
        Object item = params.get(itemArgument.getName()).getValue();

        if (Publishers.isConvertibleToPublisher(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            if (Publisher.class.isAssignableFrom(itemArgument.getType())) {
                return (Publisher<T>) item;
            }
            return Publishers.convertPublisher(conversionService, item, Publisher.class);
        }

        if (itemArgument.getType().isArray() && type.isAssignableFrom(itemArgument.getType().getComponentType())) {
            return Flux.fromArray((T[]) item);
        }

        if (Iterable.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return Flux.fromIterable((Iterable<T>) item);
        }

        return Flux.just((T) item);
    }

    public boolean hasSortKey() {
        return sortKey != null;
    }


    boolean isValid() {
        return partitionKey != null;
    }

    public Object getPartitionValue(Map<String, MutableArgumentValue<?>> params) {
        return params.get(partitionKey.getName()).getValue();
    }

    public Object getSortValue(Map<String, MutableArgumentValue<?>> params) {
        return sortKey == null ? null : params.get(sortKey.getFirstArgument().getName()).getValue();
    }

    public Publisher<?> getSortAttributeValues(ConversionService conversionService, Map<String, MutableArgumentValue<?>> params) {
        return sortKey == null ? Flux.empty() : toPublisher(conversionService, Object.class, sortKey.getFirstArgument(), params);
    }


    public <T> Consumer<QueryBuilder<T>> generateQuery(MethodInvocationContext<Object, Object> context) {
        return q -> {
            if (index != null) {
                q.index(index);
            }

            q.partitionKey(getPartitionValue(context.getParameters()));

            Object sortValue = getSortValue(context.getParameters());
            Object secondSortValue = sortKey == null || sortKey.getSecondArgument() == null ? null : context.getParameters().get(sortKey.getSecondArgument().getName()).getValue();

            if (sortValue != null) {
                q.sortKey(s -> sortKey.getOperator().apply(s, sortKey.getName(), sortValue, secondSortValue));
            }

            if (consistent) {
                q.consistent(Builders.Read.READ);
            }

            if (descending) {
                q.order(Builders.Sort.DESC);
            }

            if (!filters.isEmpty()) {
                filters.forEach((name, filter) -> {
                    Object firstValue = context.getParameters().get(filter.getFirstArgument().getName()).getValue();
                    Object secondValue = filter.getSecondArgument() == null ? null : context.getParameters().get(filter.getSecondArgument().getName()).getValue();

                    if (firstValue == null && !filter.isRequired()) {
                        return;
                    }

                    q.filter(f -> filter.getOperator().apply(
                        f,
                        name,
                        firstValue,
                        secondValue)
                    );
                });
            }
        };
    }

    public boolean isSortKeyPublisherOrIterable() {
        return sortKey.getFirstArgument().getType().isArray() || Iterable.class.isAssignableFrom(sortKey.getFirstArgument().getType()) || Publisher.class.isAssignableFrom(sortKey.getFirstArgument().getType());
    }

    public boolean isCustomized() {
        return index != null || consistent || descending || !filters.isEmpty() || sortKey != null && sortKey.getOperator() != Filter.Operator.EQ;
    }
}
