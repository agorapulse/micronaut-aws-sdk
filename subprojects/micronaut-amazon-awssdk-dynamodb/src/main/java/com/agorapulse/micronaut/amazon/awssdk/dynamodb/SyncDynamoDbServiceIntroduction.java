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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Scan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.ItemArgument;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.QueryArguments;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Introduction for {@link Service} annotation.
 *
 * @deprecated this service will be replaced by {@link AsyncDynamoDbServiceIntroduction} once matured.
 */
@Singleton
@Deprecated
public class SyncDynamoDbServiceIntroduction implements DynamoDbServiceIntroduction {

    private final FunctionEvaluator functionEvaluator;
    private final DynamoDBServiceProvider provider;
    private final ConversionService conversionService;

    public SyncDynamoDbServiceIntroduction(
        FunctionEvaluator functionEvaluator,
        DynamoDBServiceProvider provider, ConversionService conversionService
    ) {
        this.functionEvaluator = functionEvaluator;
        this.provider = provider;
        this.conversionService = conversionService;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> Object doIntercept(MethodInvocationContext<Object, Object> context, Class<T> type, String tableName) {
        DynamoDbService<T> service = provider.findOrCreate(tableName, type);

        try {
            return doIntercept(context, service);
        } catch (ResourceNotFoundException ignored) {
            service.createTable();
            return doIntercept(context, service);
        }
    }

    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, DynamoDbService<T> service) {
        String methodName = context.getMethodName();
        if (methodName.startsWith("save")) {
            return handleSave(service, context);
        }

        if (methodName.startsWith("get") || methodName.startsWith("load")) {
            return handleGet(service, context);
        }

        if (context.getTargetMethod().isAnnotationPresent(Query.class)) {
            DetachedQuery<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Query.class).value(), context);

            if (methodName.startsWith("count")) {
                return service.count(criteria);
            }

            Publisher<T> queryResult = service.query(criteria);
            if (methodName.startsWith("delete")) {
                return service.deleteAll(queryResult);
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T, ?> update = (UpdateBuilder<T, ?>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return service.updateAll(queryResult, update);
            }

            return publisherOrIterable(queryResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Scan.class)) {
            DetachedScan<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Scan.class).value(), context);

            if (methodName.startsWith("count")) {
                return service.count(criteria);
            }

            Publisher<T> scanResult = service.scan(criteria);

            if (methodName.startsWith("delete")) {
                return service.deleteAll(scanResult);
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T, ?> update = (UpdateBuilder<T, ?>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return service.updateAll(scanResult, update);
            }

            return publisherOrIterable(scanResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
            DetachedUpdate<T, ?> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);

            return service.update(criteria);
        }

        if (methodName.startsWith("delete")) {
            Optional<ItemArgument> maybeItemArgument = ItemArgument.findItemArgument(service.getItemType(), context);
            if (maybeItemArgument.isPresent()) {
                return handleDelete(service, context, maybeItemArgument);
            }
        }

        if (methodName.startsWith("query") || methodName.startsWith("findAll") || methodName.startsWith("list") || methodName.startsWith("count") || methodName.startsWith("delete")) {
            QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata(), service.getItemType());
            if (methodName.startsWith("count")) {
                if (partitionAndSort.isCustomized()) {
                    return service.countUsingQuery(partitionAndSort.generateQuery(context, conversionService));
                }
                return service.count(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters()));
            }
            if (methodName.startsWith("delete")) {
                if (partitionAndSort.isCustomized()) {
                    return service.deleteAll(service.query(partitionAndSort.generateQuery(context, conversionService)));
                }
                Optional<ItemArgument> maybeItemArgument = ItemArgument.findItemArgument(service.getItemType(), context);
                return handleDelete(service, context, maybeItemArgument);
            }
            if (partitionAndSort.isCustomized()) {
                return publisherOrIterable(service.query(partitionAndSort.generateQuery(context, conversionService)), context.getReturnType().getType());
            }
            return publisherOrIterable(
                service.findAll(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters())),
                context.getReturnType().getType()
            );
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod().getTargetMethod());
    }

    private Object publisherOrIterable(Publisher<?> result, Class<?> type) {
        if (Publishers.isConvertibleToPublisher(type)) {
            return Publishers.convertPublisher(conversionService, result, type);
        }

        return Flux.from(result).collectList().blockOptional().orElse(Collections.emptyList());
    }

    private <T> Object handleSave(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length != 1) {
            throw new UnsupportedOperationException("Method expects 1 parameter - item, iterable of items or array of items");
        }

        Argument<?> itemArgument = args[0];
        Publisher<T> items = QueryArguments.toPublisher(conversionService, service.getItemType(), itemArgument, params);

        if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
            return publisherOrIterable(service.saveAll(items), context.getReturnType().getType());
        }

        return service.save((T) params.get(itemArgument.getName()).getValue());
    }

    private <T> Object handleDelete(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context, Optional<ItemArgument> maybeItemArgument) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();

        if (maybeItemArgument.isPresent()) {
            ItemArgument itemArgument = maybeItemArgument.get();
            Publisher<T> items = QueryArguments.toPublisher(conversionService, service.getItemType(), itemArgument.getArgument(), params);

            if (!itemArgument.isSingle()) {
                return service.deleteAll(items);
            }

            if (service.getItemType().isAssignableFrom(itemArgument.getArgument().getType())) {
                return service.delete(Flux.from(items).blockFirst());
            }
        }

        Argument<?>[] args = context.getArguments();
        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - partition key and sort key, an item or items");
        }

        QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata(), service.getItemType());
        service.delete(partitionAndSort.getPartitionValue(params), partitionAndSort.getSortValue(params));
        return 1;
    }

    private <T> Object handleGet(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - partition key and sort key or sort keys");
        }

        QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata(), service.getItemType());
        Object partitionValue = partitionAndSort.getPartitionValue(params);

        if (!partitionAndSort.hasSortKey()) {
            if (partitionAndSort.isPartitionKeyPublisherOrIterable()) {
                return publisherOrIterable(service.getAll(partitionAndSort.getPartitionAttributeValues(conversionService, params)), context.getReturnType().getType());
            }
            return service.get(partitionValue, null);
        }

        if (partitionAndSort.isSortKeyPublisherOrIterable()) {
            Publisher<T> all = service.getAll(partitionValue, partitionAndSort.getSortAttributeValues(conversionService, params));
            return publisherOrIterable(all, context.getReturnType().getType());
        }

        return service.get(partitionValue, partitionAndSort.getSortValue(params));
    }

}
