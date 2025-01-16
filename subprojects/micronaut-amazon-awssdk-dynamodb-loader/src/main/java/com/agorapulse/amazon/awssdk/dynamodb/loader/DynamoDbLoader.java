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
package com.agorapulse.amazon.awssdk.dynamodb.loader;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Loads data into DynamoDB.
 */
public interface DynamoDbLoader {

    /**
     * Loads data into DynamoDB and publishes the loaded items as they are loaded.
     *
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the mappings the map containing class as keys and file names as values
     * @return the publisher of loaded items
     */
    Publisher<Object> load(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings);

    /**
     * Loads data into DynamoDB and waits for the completion.
     *
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the mappings the map containing class as keys and file names as values
     */
    default void loadAll(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings) {
        Flux.from(load(fileLoader, mappings)).blockLast();
    }



}
