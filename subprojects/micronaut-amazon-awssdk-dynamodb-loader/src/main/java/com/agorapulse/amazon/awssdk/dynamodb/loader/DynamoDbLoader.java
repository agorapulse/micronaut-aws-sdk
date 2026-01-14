/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
import java.util.stream.Stream;

/**
 * Loads data into DynamoDB.
 */
public interface DynamoDbLoader {

    /**
     * Loads data into DynamoDB and publishes the loaded items as they are loaded.
     *
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the map containing class as keys and file names as values
     * @return the publisher of loaded items
     */
    Publisher<Object> load(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings);

    /**
     * Loads data into DynamoDB and waits for the completion.
     *
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the map containing class as keys and file names as values
     */
    default void loadAll(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings) {
        Flux.from(load(fileLoader, mappings)).blockLast();
    }

    /**
     * Reads and parses data from files without saving to DynamoDB.
     * This is useful for tests that need the parsed objects but don't want to persist them.
     *
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the map containing class as keys and file names as values
     * @return the publisher of parsed items (not saved to DynamoDB)
     */
    Publisher<Object> read(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings);

    /**
     * Reads and parses data from files without saving to DynamoDB.
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param mappings the map containing class as keys and file names as values
     * @return the parsed items (not saved to DynamoDB)
     */
    default Stream<Object> readAll(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings) {
        return Flux.from(read(fileLoader, mappings)).toStream();
    }

    /**
     * Reads and parses data from files without saving to DynamoDB.
     * @param fileLoader the file loader function that takes the file name and returns the content of the file
     * @param entityType the type of the entity to read
     * @param files the files to read
     * @return the parsed items (not saved to DynamoDB)
     */
    default <T> Stream<T> readAll(UnaryOperator<String> fileLoader, Class<T> entityType, Iterable<String> files) {
        return readAll(fileLoader, Map.of(entityType, files)).map(entityType::cast);
    }

}
