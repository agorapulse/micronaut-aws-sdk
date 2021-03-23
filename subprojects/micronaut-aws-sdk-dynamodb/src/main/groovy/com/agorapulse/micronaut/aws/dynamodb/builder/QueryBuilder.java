/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Builder for DynamoDB queries.
 * @param <T> type of the DynamoDB entity
 */
public interface QueryBuilder<T> extends DetachedQuery<T> {

    /**
     * Sort the results by the range index
     * @param sort the sort keyword
     * @return self
     */
    QueryBuilder<T> sort(Builders.Sort sort);

    /**
     * Demand consistent reads.
     * @param read the read keyword
     * @return self
     */
    QueryBuilder<T> consistent(Builders.Read read);

    /**
     * Demand inconsistent reads.
     * @param read the read keyword
     * @return self
     */
    QueryBuilder<T> inconsistent(Builders.Read read);

    /**
     * Select the index on which this query will be executed.
     * @param name the name of the index to be used
     * @return self
     */
    QueryBuilder<T> index(String name);

    /**
     * Sets the hash key value for the query.
     *
     * This parameter is required for every query.
     *
     * @param key the hash key of the query or an instance of the object with the hash key set
     * @return self
     */
    QueryBuilder<T> hash(Object key);

    /**
     * One or more range key conditions.
     * @param conditions consumer to build the conditions
     * @return self
     */
    QueryBuilder<T> range(Consumer<RangeConditionCollector<T>> conditions);

    /**
     * One or more range key conditions.
     * @param conditions closure to build the conditions
     * @return self
     */
    default QueryBuilder<T> range(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return range(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    QueryBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    /**
     * One or more range key filter conditions in disjunction.
     * <p>
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    default QueryBuilder<T> or(Consumer<RangeConditionCollector<T>> conditions) {
        filter(ConditionalOperator.OR);
        return filter(conditions);
    }

    /**
     * One or more range key filter conditions in conjunction.
     * <p>
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    default QueryBuilder<T> and(Consumer<RangeConditionCollector<T>> conditions) {
        filter(ConditionalOperator.AND);
        return filter(conditions);
    }

    /**
     * One or more range key filter conditions.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default QueryBuilder<T> filter(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions in disjunction.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default QueryBuilder<T> or(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return or(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions in conjunction.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default QueryBuilder<T> and(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return and(ConsumerWithDelegate.create(conditions));
    }

    /**
     * Sets the conditional operator for the filter.
     *
     * Default is <code>and</code>
     *
     * @param or the conditional operator, usually <code>or</code> to switch to disjunction of filter conditions
     * @return self
     */
    QueryBuilder<T> filter(ConditionalOperator or);

    /**
     * Sets the desired pagination of the queries.
     *
     * This only sets the optimal pagination of the queries and does not limit the number of items returned.
     *
     * Use <code>{@link io.reactivex.Flowable#take(long)}</code> to limit the number results returned from the query.
     *
     * @param page number of entities loaded by one query request (not a number of total entities returned)
     * @return self
     */
    QueryBuilder<T> page(int page);

    /**
     * Sets the maximum number of items to be returned from the queries.
     *
     * This is a shortcut for calling <code>{@link io.reactivex.Flowable#take(long)}</code> on the result Flowable.
     *
     * @param max the maximum number of items returned
     * @return self
     */
    QueryBuilder<T> limit(int max);


    /**
     * Sets the query offset by defining the exclusive start hash and range key (hash and range key of the last entity returned).
     * @param exclusiveStartKeyValue exclusive start key hash value
     * @param exclusiveRangeStartKey exclusive start key range value
     * @return self
     */
    QueryBuilder<T> offset(Object exclusiveStartKeyValue, Object exclusiveRangeStartKey);

    /**
     * Sets the query offset by defining the exclusive start hash key (hash key of the last entity returned).
     * @param exclusiveStartKeyValue exclusive start key hash value
     * @return self
     */
    default QueryBuilder<T> offset(Object exclusiveStartKeyValue) {
        return offset(exclusiveStartKeyValue, null);
    }

    /**
     * Configures the native query expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native query expression
     * @return self
     */
    QueryBuilder<T> configure(Consumer<DynamoDBQueryExpression<T>> configurer);

    /**
     * Configures the native query expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native query expression
     * @return self
     */
    default QueryBuilder<T> configure(
        @DelegatesTo(type = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression<T>")
            Closure<Object> configurer
    ) {
        return configure(ConsumerWithDelegate.create(configurer));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    QueryBuilder<T> only(Iterable<String> propertyPaths);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    default QueryBuilder<T> only(String... propertyPaths) {
        return only(Arrays.asList(propertyPaths));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param collector closure to collect the property paths
     * @return self
     */
    default QueryBuilder<T> only(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_ONLY)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> collector
    ) {
        return only(PathCollector.collectPaths(collector).getPropertyPaths());
    }

}
