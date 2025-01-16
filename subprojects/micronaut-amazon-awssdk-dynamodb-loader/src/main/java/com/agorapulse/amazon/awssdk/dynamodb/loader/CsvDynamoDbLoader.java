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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

@Singleton
@Named("csv")
public class CsvDynamoDbLoader implements DynamoDbLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvDynamoDbLoader.class);

    private final Scheduler scheduler = Schedulers.boundedElastic();
    private final ConversionService conversionService;
    private final DynamoDBServiceProvider dynamoDBServiceProvider;
    private final ObjectMapper mapper;

    public CsvDynamoDbLoader(ConversionService conversionService, DynamoDBServiceProvider dynamoDBServiceProvider, ObjectMapper mapper) {
        this.conversionService = conversionService;
        this.dynamoDBServiceProvider = dynamoDBServiceProvider;
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<Object> load(UnaryOperator<String> fileLoader, Map<Class<?>, Iterable<String>> mappings) {
        return (Publisher<Object>) Flux.fromIterable(mappings.entrySet()).parallel().runOn(scheduler).flatMap(entry ->
                Flux.fromIterable(entry.getValue()).parallel().runOn(scheduler).flatMap(filename ->
                        preload(fileLoader, entry.getKey(), filename)
                )
        ).sequential();
    }

    private <T> Flux<T> load(UnaryOperator<String> fileLoader, Class<T> type, String filename) {
        BeanIntrospection<T> introspection = BeanIntrospection.getIntrospection(type);

        long tick = System.currentTimeMillis();

        String text = fileLoader.apply(filename);

        if (text == null) {
            LOGGER.error("File {} not found", filename);
            return Flux.empty();
        }

        try {
            CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new StringReader(text));

            LOGGER.info("Loading records from {} as {}", filename, type);

            Map<String, BeanProperty<T, Object>> properties = new TreeMap<>();
            Set<String> notFound = new HashSet<>();

            return Flux.<Map<String, String>>generate(sink -> {
                try {
                    Map<String, String> data = reader.readMap();
                    if (data == null) {
                        sink.complete();
                    } else {
                        sink.next(data);
                    }
                } catch (IOException | CsvValidationException e) {
                    sink.error(e);
                }
            }).map(data -> {
                T insight = introspection.instantiate();

                data.forEach((header, value) -> {
                    if (StringUtils.isEmpty(value)) {
                        return;
                    }

                    if (notFound.contains(header)) {
                        return;
                    }

                    BeanProperty<T, Object> property = properties.computeIfAbsent(header, h -> {
                        BeanProperty<T, Object> prop = introspection.getProperty(h).orElse(null);
                        if (prop == null) {
                            notFound.add(h);
                            LOGGER.warn("Property {} not found in {} for file {}", h, type, filename);
                        }
                        return prop;
                    });

                    if (property == null) {
                        return;
                    }

                    Optional<?> convertedValue;
                    if (value.startsWith("\"{\"\"")) {
                        String cleanUpJson = value.substring(1, value.length() - 1).replace("\"\"", "\"");
                        try {
                            convertedValue = Optional.of(mapper.readValue(cleanUpJson, property.getType()));
                        } catch (JsonProcessingException e) {
                            convertedValue = Optional.empty();
                            LOGGER.error("Error parsing JSON: {} for property: {} of {}", cleanUpJson, property.getName(), type);
                        }
                    } else {
                        convertedValue = conversionService.convert(value, property.asArgument());
                    }

                    convertedValue.map(Object.class::cast).ifPresent(o -> property.set(insight, o));
                });

                return insight;
            }).doOnComplete(() -> {
                LOGGER.info("Loaded {} records from {} as {} in {} ms", reader.getRecordsRead(), filename, type, System.currentTimeMillis() - tick);
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing CSV: {}", e.getMessage());
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error parsing CSV: {}", e.getMessage());
            return Flux.empty();
        }
    }

    private <T> ParallelFlux<T> preload(UnaryOperator<String> fileLoader, Class<T> insightType, String fileName) {
        return Flux.from(dynamoDBServiceProvider.findOrCreate(insightType).saveAll(load(fileLoader, insightType, fileName))).parallel().runOn(Schedulers.boundedElastic());
    }

}
