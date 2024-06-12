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
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.QueryArguments;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.Collections;
import java.util.Map;

/**
 * Introduction for {@link Service} annotation.
 */
@Singleton
@Replaces(SyncDynamoDbServiceIntroduction.class)
@Requires(property = "aws.dynamodb.async", value = "true")
public class AsyncDynamoDbServiceIntroduction implements DynamoDbServiceIntroduction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDynamoDbServiceIntroduction.class);

    private final FunctionEvaluator functionEvaluator;
    private final AsyncDynamoDBServiceProvider provider;
    private final ConversionService conversionService;

    public AsyncDynamoDbServiceIntroduction(
        FunctionEvaluator functionEvaluator,
        AsyncDynamoDBServiceProvider provider,
        ConversionService conversionService
    ) {
        this.functionEvaluator = functionEvaluator;
        this.provider = provider;
        this.conversionService = conversionService;
    }

    @SuppressWarnings("unchecked")
    private <T> Publisher<T> toPublisher(Class<T> type, Argument<?> itemArgument, Map<String, MutableArgumentValue<?>> params) {
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

    @Override
    public <T> Object doIntercept(MethodInvocationContext<Object, Object> context, Class<T> type, String tableName) {
        AsyncDynamoDbService<T> service = provider.findOrCreate(tableName, type);

        try {
            return doIntercept(context, service);
        } catch (ResourceNotFoundException ignored) {
            service.createTable();
            return doIntercept(context, service);
        }
    }

    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, AsyncDynamoDbService<T> service) {
        String methodName = context.getMethodName();
        if (methodName.startsWith("save")) {
            return unwrapIfRequired(handleSave(service, context), context.getReturnType().getType());
        }

        if (methodName.startsWith("get") || methodName.startsWith("load")) {
            return unwrapIfRequired(handleGet(service, context), context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Query.class)) {
            DetachedQuery<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Query.class).value(), context);

            if (methodName.startsWith("count")) {
                return unwrapIfRequired(service.count(criteria), context.getReturnType().getType());
            }

            Publisher<T> queryResult = service.query(criteria);
            if (methodName.startsWith("delete")) {
                return unwrapIfRequired(service.deleteAll(queryResult), context.getReturnType().getType());
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T, ?> update = (UpdateBuilder<T, ?>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return unwrapIfRequired(service.updateAll(queryResult, update), context.getReturnType().getType());
            }

            return unwrapIfRequired(queryResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Scan.class)) {
            DetachedScan<T> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Scan.class).value(), context);

            if (methodName.startsWith("count")) {
                return unwrapIfRequired(service.count(criteria), context.getReturnType().getType());
            }

            Publisher<T> scanResult = service.scan(criteria);

            if (methodName.startsWith("delete")) {
                return unwrapIfRequired(service.deleteAll(scanResult), context.getReturnType().getType());
            }

            if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
                UpdateBuilder<T, ?> update = (UpdateBuilder<T, ?>) functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);
                return unwrapIfRequired(service.updateAll(scanResult, update), context.getReturnType().getType());
            }

            return unwrapIfRequired(scanResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
            DetachedUpdate<T, ?> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);

            return unwrapIfRequired(service.update(criteria), context.getReturnType().getType());
        }

        if (methodName.startsWith("delete")) {
            return unwrapIfRequired(handleDelete(service, context), context.getReturnType().getType());
        }

        if (methodName.startsWith("query") || methodName.startsWith("findAll") || methodName.startsWith("list") || methodName.startsWith("count")) {
            QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata());
            if (methodName.startsWith("count")) {
                if (partitionAndSort.isCustomized()) {
                    return unwrapIfRequired(service.countUsingQuery(partitionAndSort.generateQuery(context)), context.getReturnType().getType());
                }
                return unwrapIfRequired(service.count(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters())), context.getReturnType().getType());
            }
            if (partitionAndSort.isCustomized()) {
                return unwrapIfRequired(service.query(partitionAndSort.generateQuery(context)), context.getReturnType().getType());
            }
            return unwrapIfRequired(
                service.findAll(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters())),
                context.getReturnType().getType()
            );
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod().getTargetMethod());
    }

    private Object unwrapIfRequired(Publisher<?> publisher, Class<Object> type) {
        if (void.class.isAssignableFrom(type) || Void.class.isAssignableFrom(type)) {
            return Mono.from(publisher).block();
        }
        if (Publishers.isConvertibleToPublisher(type)) {
            return Publishers.convertPublisher(conversionService, publisher, type);
        }

        if (Number.class.isAssignableFrom(type) || type.isPrimitive() && !boolean.class.isAssignableFrom(type)) {
            if (Publishers.isSingle(publisher.getClass())) {
                Object result = Mono.from(publisher).block();
                return conversionService.convert(result, type).orElseGet(() -> {
                    LOGGER.warn("Cannot convert value {} to type {}", result, type);
                    return 0;
                });
            }
            Long count = Flux.from(publisher).count().block();
            return conversionService.convert(count, type).orElseGet(() -> {
                LOGGER.warn("Cannot convert value {} to type {}", count, type);
                return 0;
            });
        }

        if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return conversionService.convert(Flux.from(publisher).collectList().block(), type).orElse(Collections.emptyList());
        }

        Object value = Mono.from(publisher).block();
        return conversionService.convert(value, type).orElseGet(() -> {
            LOGGER.warn("Cannot convert value {} to type {}", value, type);
            return null;
        });
    }


    private <T> Publisher<T> handleSave(AsyncDynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length != 1) {
            throw new UnsupportedOperationException("Method expects 1 parameter - item, iterable of items or array of items");
        }

        Argument<?> itemArgument = args[0];
        Publisher<T> items = toPublisher(service.getItemType(), itemArgument, params);

        if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
            return service.saveAll(items);
        }

        return service.save((T) params.get(itemArgument.getName()).getValue());
    }

    private <T> Publisher<?> handleDelete(AsyncDynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length == 1) {
            Argument<?> itemArgument = args[0];
            Publisher<T> items = toPublisher(service.getItemType(), itemArgument, params);

            if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
                return service.deleteAll(items);
            }

            if (service.getItemType().isAssignableFrom(itemArgument.getType())) {
                return service.delete(Flux.from(items).blockFirst());
            }
        }

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - partition key and sort key, an item or items");
        }

        QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata());
        return service.delete(partitionAndSort.getPartitionValue(params), partitionAndSort.getSortValue(params));
    }

    private <T> Publisher<T> handleGet(AsyncDynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - partition key and sort key or sort keys");
        }

        QueryArguments partitionAndSort = QueryArguments.create(context, service.getTable().tableSchema().tableMetadata());
        Object partitionValue = partitionAndSort.getPartitionValue(params);

        if (!partitionAndSort.hasSortKey()) {
            return service.get(partitionValue, null);
        }

        if (partitionAndSort.isSortKeyPublisherOrIterable()) {
            return service.getAll(partitionValue, partitionAndSort.getSortAttributeValues(conversionService, params));
        }

        return service.get(partitionValue, partitionAndSort.getSortValue(params));
    }

}
