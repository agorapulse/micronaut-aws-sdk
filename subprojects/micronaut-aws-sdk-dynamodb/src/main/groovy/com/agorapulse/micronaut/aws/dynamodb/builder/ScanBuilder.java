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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalOperator;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Builder for DynamoDB scans.
 * @param <T> type of the DynamoDB entity
 */
public interface ScanBuilder<T> extends DetachedScan<T> {

    /**
     * Demand consistent reads.
     * @param read the read keyword
     * @return self
     */
    ScanBuilder<T> consistent(Builders.Read read);

    /**
     * Demand inconsistent reads.
     * @param read the read keyword
     * @return self
     */
    ScanBuilder<T> inconsistent(Builders.Read read);

    /**
     * Select the index on which this scan will be executed.
     * @param name the name of the index to be used
     * @return self
     */
    ScanBuilder<T> index(String name);

    /**
     * One or more filter conditions.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    ScanBuilder<T> filter(Consumer<RangeConditionCollector<T>> conditions);

    /**
     * One or more filter conditions in conjunction.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    default ScanBuilder<T> and(Consumer<RangeConditionCollector<T>> conditions) {
        filter(ConditionalOperator.AND);
        return filter(conditions);
    }

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions consumer to build the conditions
     * @return self
     */
    default ScanBuilder<T> or(Consumer<RangeConditionCollector<T>> conditions) {
        filter(ConditionalOperator.OR);
        return filter(conditions);
    }

    /**
     * One or more filter conditions.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default ScanBuilder<T> filter(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return filter(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default ScanBuilder<T> or(
        @DelegatesTo(type = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>")
            Closure<RangeConditionCollector<T>> conditions
    ) {
        return or(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more filter conditions in conjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    default ScanBuilder<T> and(
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
    ScanBuilder<T> filter(ConditionalOperator or);

    /**
     * Sets the desired pagination of the scans.
     *
     * This only sets the optimal pagination of the scans and does not limit the number of items returned.
     *
     * Use <code>{@link io.reactivex.Flowable#take(long)}</code> to limit the number results returned from the scan.
     *
     * @param page number of entities loaded by one scan request (not a number of total entities returned)
     * @return self
     */
    ScanBuilder<T> page(int page);

    /**
     * Sets the maximum number of items to be returned from the queries.
     *
     * This is a shortcut for calling <code>{@link io.reactivex.Flowable#take(long)}</code> on the result Flowable.
     *
     * @param max the maximum number of items returned
     * @return self
     */
    ScanBuilder<T> limit(int max);

    /**
     * Sets the scan offset by defining the exclusive start hash and range key (hash and range key of the last entity returned).
     * @param exclusiveStartKeyValue exclusive start key hash value
     * @param exclusiveRangeStartKey exclusive start key range value
     * @return self
     */
    ScanBuilder<T> offset(Object exclusiveStartKeyValue, Object exclusiveRangeStartKey);

    /**
     * Sets the scan offset by defining the exclusive start hash key (hash key of the last entity returned).
     * @param exclusiveStartKeyValue exclusive start key hash value
     * @return self
     */
    default ScanBuilder<T> offset(Object exclusiveStartKeyValue) {
        return offset(exclusiveStartKeyValue, null);
    }

    /**
     * Configures the native scan expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native scan expression
     * @return self
     */
    ScanBuilder<T> configure(Consumer<DynamoDBScanExpression> configurer);

    /**
     * Configures the native scan expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native scan expression
     * @return self
     */
    default ScanBuilder<T> configure(
        @DelegatesTo(type = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression")
            Closure<Object> configurer
    ) {
        return configure(ConsumerWithDelegate.create(configurer));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    ScanBuilder<T> only(Iterable<String> propertyPaths);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    default ScanBuilder<T> only(String... propertyPaths) {
        return only(Arrays.asList(propertyPaths));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param collector closure to collect the property paths
     * @return self
     */
    default ScanBuilder<T> only(
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_ONLY)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> collector
    ) {
        return only(PathCollector.collectPaths(collector).getPropertyPaths());
    }

}
