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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.*;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.cleanAttributeName;

public class QueryConditionalFactory {

    // TODO: methods as parts of comparison

    public static QueryConditional attributeExists(String path) {
        return new ConditionalWithSingleStringArgument(ConditionalWithSingleStringArgument.ATTRIBUTE_EXISTS, path);
    }

    public static QueryConditional attributeNotExists(String path) {
        return new ConditionalWithSingleStringArgument(ConditionalWithSingleStringArgument.ATTRIBUTE_NOT_EXISTS, path);
    }

    public static QueryConditional size(String path) {
        return new ConditionalWithSingleStringArgument(ConditionalWithSingleStringArgument.SIZE, path);
    }

    public static QueryConditional between(String property, AttributeValue lowerBound, AttributeValue upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, lowerBound, upperBound);
    }

    public static QueryConditional inList(String property, List<AttributeValue> values) {
        return new InListConditional(property, values);
    }

    public static QueryConditional equalTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional notEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional lessThan(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, value);
    }

    public static QueryConditional lessThanOrEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional greaterThan(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, value);
    }

    public static QueryConditional greaterThanOrEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional sizeEqualTo(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, size(path), value);
    }

    public static QueryConditional sizeNotEqualTo(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, size(path), value);
    }

    public static QueryConditional sizeLessThan(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, size(path), value);
    }

    public static QueryConditional sizeLessThanOrEqualTo(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, size(path), value);
    }

    public static QueryConditional sizeGreaterThan(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, size(path), value);
    }

    public static QueryConditional sizeGreaterThanOrEqualTo(String path, AttributeValue value) {
        return new ConditionalWithConditionalAndArgument(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, size(path), value);
    }

    public static QueryConditional contains(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, value);
    }

    public static QueryConditional beginsWith(String property, String substring) {
        return beginsWith(property, stringValue(substring));
    }

    public static QueryConditional beginsWith(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.BEGINS_WITH_TEMPLATE, property, value);
    }

    public static QueryConditional attributeType(String property, AttributeValue type) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, type);
    }

    public static QueryConditional attributeType(String property, AttributeValueType type) {
        return attributeType(property, stringValue(type.name()));
    }

    public static QueryConditional attributeType(String property, String type) {
        if (Arrays.stream(AttributeValueType.values()).noneMatch(t -> t.name().equals(type))) {
            throw new IllegalArgumentException("Unrecognized type: " + type);
        }
        return attributeType(property, stringValue(type));
    }

    public static QueryConditional attributeTypeIsNull(String property) {
        return attributeType(property, AttributeValueType.NULL);
    }

    public static QueryConditional attributeType(String property, Class<?> type) {
        return attributeType(property, EnhancedType.of(type));
    }

    public static QueryConditional attributeType(String property, EnhancedType<?> type) {
        if (type == null) {
            return attributeType(property, AttributeValueType.NULL);
        }

        if (CharSequence.class.isAssignableFrom(type.rawClass())) {
            return attributeType(property, AttributeValueType.S);
        }

        if (Number.class.isAssignableFrom(type.rawClass())) {
            return attributeType(property, AttributeValueType.N);
        }

        if (Boolean.class.isAssignableFrom(type.rawClass())) {
            return attributeType(property, AttributeValueType.BOOL);
        }

        if (SdkBytes.class.isAssignableFrom(type.rawClass())) {
            return attributeType(property, AttributeValueType.B);
        }

        if (Map.class.isAssignableFrom(type.rawClass())) {
            return attributeType(property, AttributeValueType.M);
        }

        if (Iterable.class.isAssignableFrom(type.rawClass()) && type.rawClassParameters().size() > 0) {
            Class<?> rawClassParameter = type.rawClassParameters().get(0).rawClass();
            if (CharSequence.class.isAssignableFrom(rawClassParameter)) {
                return attributeType(property, stringValue("SS"));
            }

            if (Number.class.isAssignableFrom(rawClassParameter)) {
                return attributeType(property, stringValue("NS"));
            }

            if (SdkBytes.class.isAssignableFrom(rawClassParameter)) {
                return attributeType(property, stringValue("BS"));
            }
            return attributeType(property, AttributeValueType.L);
        }

        throw new IllegalArgumentException("Cannot determine expected attribute type from " + type);
    }

    public static QueryConditional group(QueryConditional statement) {
        if (statement instanceof GroupConditional) {
            return statement;
        }
        return new GroupConditional(statement);
    }

    public static QueryConditional and(Collection<QueryConditional> statements) {
        if (statements.size() == 1) {
            return statements.iterator().next();
        }
        return new LogicalConditional(LogicalConditional.AND_TOKEN, statements);
    }

    public static QueryConditional and(QueryConditional... statements) {
        return and(Arrays.asList(statements));
    }

    public static QueryConditional or(Collection<QueryConditional> statements) {
        if (statements.size() == 1) {
            return statements.iterator().next();
        }
        return new LogicalConditional(LogicalConditional.OR_TOKEN, statements);
    }

    public static QueryConditional or(QueryConditional... statements) {
        return or(Arrays.asList(statements));
    }

    public static QueryConditional not(QueryConditional statement) {
        return new NotConditional(statement);
    }

    // package private

    static String expressionKey(String key) {
        return "#AGORA_MAPPED_" + cleanAttributeName(key) + "_" + RANDOM.nextInt(10000);
    }

    static String expressionValue(String key) {
        return ":AGORA_MAPPED_" + cleanAttributeName(key) + "_" + RANDOM.nextInt(10000);
    }

    // private

    private static final Random RANDOM = new Random();
}
