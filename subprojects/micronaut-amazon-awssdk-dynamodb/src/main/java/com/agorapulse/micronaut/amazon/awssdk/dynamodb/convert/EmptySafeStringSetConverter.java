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

import io.micronaut.core.util.CollectionUtils;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashSet;
import java.util.Set;

/**
 * Set string datatype in dynamo does not allow empty values.
 * Without this custom converter if an empty set is present in entity an error will occur : 'dynamodb an string set may not be empty'
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html">...</a>
 */
public class EmptySafeStringSetConverter implements AttributeConverter<Set<String>> {
    @Override
    public AttributeValue transformFrom(Set<String> set) {
        return CollectionUtils.isEmpty(set)
                ? AttributeValues.nullAttributeValue()
                : AttributeValue.fromSs(set.stream().toList());
    }

    @Override
    public Set<String> transformTo(AttributeValue rawValue) {
        return new HashSet<>(rawValue.ss());
    }

    @Override
    public EnhancedType<Set<String>> type() {
        return EnhancedType.setOf(String.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.SS;
    }
}
