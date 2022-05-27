/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.HashKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.RangeKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Scan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedUpdate;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.UpdateBuilder;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableArgumentValue;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Introduction for {@link com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service} annotation.
 */
@Singleton
public class ServiceIntroduction implements MethodInterceptor<Object, Object> {

    private static final String PARTITION = "partition";
    private static final String SORT = "sort";
    private static final String HASH = "hash";
    private static final String RANGE = "range";

    private static class PartitionAndSort {
        Argument<?> partitionKey;
        Argument<?> sortKey;

        boolean isValid() {
            return partitionKey != null;
        }

        Object getPartitionValue(Map<String, MutableArgumentValue<?>> params) {
            return params.get(partitionKey.getName()).getValue();
        }

        Object getSortValue(Map<String, MutableArgumentValue<?>> params) {
            return sortKey == null ? null : params.get(sortKey.getName()).getValue();
        }

        Publisher<?> getSortAttributeValues(Map<String, MutableArgumentValue<?>> params) {
            return sortKey == null ? Flux.empty() : toPublisher(Object.class, sortKey, params);
        }

    }

    private final FunctionEvaluator functionEvaluator;
    private final DynamoDBServiceProvider provider;

    public ServiceIntroduction(
        FunctionEvaluator functionEvaluator,
        DynamoDBServiceProvider provider
    ) {
        this.functionEvaluator = functionEvaluator;
        this.provider = provider;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<Service> serviceAnnotationValue = context.getAnnotation(Service.class);

        if (serviceAnnotationValue == null) {
            throw new IllegalStateException("Invocation context is missing required annotation Service");
        }

        return doIntercept(context, serviceAnnotationValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> Publisher<T> toPublisher(Class<T> type, Argument<?> itemArgument, Map<String, MutableArgumentValue<?>> params) {
        Object item = params.get(itemArgument.getName()).getValue();

        if (itemArgument.getType().isArray() && type.isAssignableFrom(itemArgument.getType().getComponentType())) {
            return Flux.fromArray((T[])item);
        }

        if (Iterable.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return Flux.fromIterable((Iterable<T>) item);
        }

        if (Publisher.class.isAssignableFrom(itemArgument.getType()) && type.isAssignableFrom(itemArgument.getTypeParameters()[0].getType())) {
            return (Publisher<T>) item;
        }

        return Flux.just((T) item);
    }

    @SuppressWarnings("unchecked")
    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, AnnotationValue<Service> serviceAnnotationValue) {
        Class<T> type = (Class<T>) serviceAnnotationValue.classValue().orElseThrow(() -> new IllegalArgumentException("Annotation is missing the type value!"));
        String tableName = serviceAnnotationValue.stringValue("tableName").orElseGet(type::getSimpleName);

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

            return publisherOrList(queryResult, context.getReturnType().getType());
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

            return publisherOrList(scanResult, context.getReturnType().getType());
        }

        if (context.getTargetMethod().isAnnotationPresent(Update.class)) {
            DetachedUpdate<T, ?> criteria = functionEvaluator.evaluateAnnotationType(context.getTargetMethod().getAnnotation(Update.class).value(), context);

            return service.update(criteria);
        }

        if (methodName.startsWith("delete")) {
            return handleDelete(service, context);
        }

        if (methodName.startsWith("query") || methodName.startsWith("findAll") || methodName.startsWith("list") || methodName.startsWith("count")) {
            PartitionAndSort partitionAndSort = findHashAndRange(context.getArguments(), service);
            if (methodName.startsWith("count")) {
                return service.count(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters()));
            }
            return publisherOrList(
                service.findAll(partitionAndSort.getPartitionValue(context.getParameters()), partitionAndSort.getSortValue(context.getParameters())),
                context.getReturnType().getType()
            );
        }

        throw new UnsupportedOperationException("Cannot implement method " + context.getExecutableMethod().getTargetMethod());
    }

    private Object publisherOrList(Publisher<?> result, Class<?> type) {
        if (List.class.isAssignableFrom(type)) {
            return Flux.from(result).collectList().blockOptional().orElse(Collections.emptyList());
        }
        return result;
    }

    private <T> Object handleSave(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length != 1) {
            throw new UnsupportedOperationException("Method expects 1 parameter - item, iterable of items or array of items");
        }

        Argument<?> itemArgument = args[0];
        Publisher<T> items = toPublisher(service.getItemType(), itemArgument, params);

        if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
            return publisherOrList(service.saveAll(items), context.getReturnType().getType());
        }

        return service.save(Flux.from(items).blockFirst());
    }

    private <T> Object handleDelete(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
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

        PartitionAndSort partitionAndSort = findHashAndRange(args, service);
        service.delete(partitionAndSort.getPartitionValue(params), partitionAndSort.getSortValue(params));
        return 1;
    }

    private <T> Object handleGet(DynamoDbService<T> service, MethodInvocationContext<Object, Object> context) {
        Map<String, MutableArgumentValue<?>> params = context.getParameters();
        Argument<?>[] args = context.getArguments();

        if (args.length > 2) {
            throw new UnsupportedOperationException("Method expects at most 2 parameters - partition key and sort key or sort keys");
        }

        PartitionAndSort partitionAndSort = findHashAndRange(args, service);
        Object partitionValue = partitionAndSort.getPartitionValue(params);

        if (partitionAndSort.sortKey == null) {
            return service.get(partitionValue, null);
        }

        if (
            partitionAndSort.sortKey.getType().isArray()
                || Iterable.class.isAssignableFrom(partitionAndSort.sortKey.getType())
                || Publisher.class.isAssignableFrom(partitionAndSort.sortKey.getType())
        ) {
            Publisher<T> all = service.getAll(partitionValue, partitionAndSort.getSortAttributeValues(params));
            return publisherOrList(all, context.getReturnType().getType());
        }

        return service.get(partitionValue, partitionAndSort.getSortValue(params));
    }

    private PartitionAndSort findHashAndRange(Argument<?>[] arguments, DynamoDbService<?> table) {
        PartitionAndSort names = new PartitionAndSort();
        for (Argument<?> argument : arguments) {
            if (
                argument.isAnnotationPresent(SortKey.class)
                    || argument.isAnnotationPresent(RangeKey.class)
                    || argument.getName().toLowerCase().contains(SORT)
                    || argument.getName().toLowerCase().contains(RANGE)
                    || argument.getName().equals(table.getTable().tableSchema().tableMetadata().primarySortKey().orElse(SORT))
            ) {
                names.sortKey = argument;
            } else if (
                argument.isAnnotationPresent(PartitionKey.class)
                    || argument.isAnnotationPresent(HashKey.class)
                    || argument.getName().toLowerCase().contains(PARTITION)
                    || argument.getName().toLowerCase().contains(HASH)
                    || argument.getName().equals(table.getTable().tableSchema().tableMetadata().primaryPartitionKey())
            ) {
                names.partitionKey = argument;
            }
        }

        if (!names.isValid()) {
            throw new UnsupportedOperationException("Method needs to have at least one argument annotated with @PartitionKey or with called 'partition'");
        }

        return names;
    }

}
