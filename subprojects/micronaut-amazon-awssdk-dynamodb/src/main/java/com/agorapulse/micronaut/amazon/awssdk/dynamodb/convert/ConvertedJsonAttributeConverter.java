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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.micronaut.jackson.ObjectMapperFactory;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;

/**
 * Converter which converts objects to JSON strings.
 * @param <T> the type of the object
 */
public class ConvertedJsonAttributeConverter<T> implements AttributeConverter<T> {

        private static final ObjectWriter OBJECT_WRITER;
        private static final ObjectReader OBJECT_READER;

        static {
            ObjectMapper mapper = new ObjectMapperFactory().objectMapper(null, null);
            OBJECT_READER = mapper.reader();
            OBJECT_WRITER = mapper.writer();
        }

        private final Class<T> type;

        public ConvertedJsonAttributeConverter(Class<T> type) {
            this.type = type;
        }

        @Override
        public AttributeValue transformFrom(T input) {
            try {
                return AttributeValue.fromS(OBJECT_WRITER.writeValueAsString(input));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Cannot write value as JSON: " + input, e);
            }
        }

        @Override
        public T transformTo(AttributeValue input) {
            try {
                return OBJECT_READER.readValue(input.s(), type);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read value: " + input.s(),  e);
            }
        }

        @Override
        public EnhancedType<T> type() {
            return EnhancedType.of(type);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }
    }
