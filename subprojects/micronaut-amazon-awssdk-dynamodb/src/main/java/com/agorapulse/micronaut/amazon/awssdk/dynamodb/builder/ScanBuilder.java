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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import java.util.Arrays;
import java.util.Collection;
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
    ScanBuilder<T> filter(Consumer<FilterConditionCollector<T>> conditions);

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
     * Sets the scan offset by defining the exclusive start value.
     * @param lastEvaluatedKey exclusive start value
     * @return self
     */
    ScanBuilder<T> lastEvaluatedKey(Object lastEvaluatedKey);

    /**
     * Configures the native scan expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer consumer to configure the native scan expression
     * @return self
     */
    ScanBuilder<T> configure(Consumer<ScanEnhancedRequest.Builder> configurer);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    ScanBuilder<T> only(Collection<String> propertyPaths);

    /**
     * Limits which properties of the returned entities will be populated.
     * @param propertyPaths property paths to be populated in the returned entities
     * @return self
     */
    default ScanBuilder<T> only(String... propertyPaths) {
        return only(Arrays.asList(propertyPaths));
    }

}
