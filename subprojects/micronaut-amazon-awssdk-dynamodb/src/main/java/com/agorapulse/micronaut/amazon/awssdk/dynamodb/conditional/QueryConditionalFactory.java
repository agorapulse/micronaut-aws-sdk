package com.agorapulse.micronaut.amazon.awssdk.dynamodb.conditional;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    public static QueryConditional between(String property, Supplier<AttributeValue> lowerBound, Supplier<AttributeValue> upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, lowerBound, upperBound);
    }

    public static QueryConditional between(String property, AttributeValue lowerBound, AttributeValue upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, just(lowerBound), just(upperBound));
    }

    public static QueryConditional between(String property, String lowerBound, String upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, just(stringValue(lowerBound)), just(stringValue(upperBound)));
    }

    public static QueryConditional between(String property, Number lowerBound, Number upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, just(numberValue(lowerBound)), just(numberValue(upperBound)));
    }

    public static QueryConditional between(String property, SdkBytes lowerBound, SdkBytes upperBound) {
        return new ConditionalWithThreeArguments(ConditionalWithThreeArguments.BETWEEN_TEMPLATE, property, just(binaryValue(lowerBound)), just(binaryValue(upperBound)));
    }

    public static QueryConditional inList(String property, Object... values) {
        return new InListConditional(property, Arrays.stream(values).map(v -> {
            if (v instanceof Supplier) {
                return (Supplier<AttributeValue>) v;
            }
            if (v instanceof Number) {
                return just(AttributeValues.numberValue((Number) v));
            }
            if (v instanceof String) {
                return just(AttributeValues.stringValue((String) v));
            }
            if (v instanceof SdkBytes) {
                return just(AttributeValues.binaryValue((SdkBytes) v));
            }
            throw new IllegalArgumentException("Cannot map value " + v + " to Supplier<AttributeValue>");
        }).collect(Collectors.toList()));
    }

    public static QueryConditional inList(String property, List<?> values) {
        return new InListConditional(property, values.stream().map(v -> {
            if (v instanceof Supplier) {
                return (Supplier<AttributeValue>) v;
            }
            if (v instanceof Number) {
                return just(AttributeValues.numberValue((Number) v));
            }
            if (v instanceof String) {
                return just(AttributeValues.stringValue((String) v));
            }
            if (v instanceof SdkBytes) {
                return just(AttributeValues.binaryValue((SdkBytes) v));
            }
            throw new IllegalArgumentException("Cannot map value " + v + " to Supplier<AttributeValue>");
        }).collect(Collectors.toList()));
    }

    public static QueryConditional equalTo(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional equalTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, just(value));
    }

    public static QueryConditional equalTo(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional equalTo(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional equalTo(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.EQUAL_TO_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional notEqualTo(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional notEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, just(value));
    }

    public static QueryConditional notEqualTo(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional notEqualTo(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional notEqualTo(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.NOT_EQUAL_TO_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional lessThan(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, value);
    }

    public static QueryConditional lessThan(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, just(value));
    }

    public static QueryConditional lessThan(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional lessThan(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional lessThan(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional lessThanOrEqualTo(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional lessThanOrEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, just(value));
    }

    public static QueryConditional lessThanOrEqualTo(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional lessThanOrEqualTo(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional lessThanOrEqualTo(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.LESS_THAN_OR_EQUAL_TO_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional greaterThan(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, value);
    }

    public static QueryConditional greaterThan(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, just(value));
    }

    public static QueryConditional greaterThan(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional greaterThan(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional greaterThan(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional greaterThanOrEqualTo(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, value);
    }

    public static QueryConditional greaterThanOrEqualTo(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, just(value));
    }

    public static QueryConditional greaterThanOrEqualTo(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional greaterThanOrEqualTo(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional greaterThanOrEqualTo(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.GREATER_THAN_OR_EQUAL_TO_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional contains(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, value);
    }

    public static QueryConditional contains(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, just(value));
    }

    public static QueryConditional contains(String property, String value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, just(stringValue(value)));
    }

    public static QueryConditional contains(String property, Number value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, just(numberValue(value)));
    }

    public static QueryConditional contains(String property, SdkBytes value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.CONTAINS_TEMPLATE, property, just(binaryValue(value)));
    }

    public static QueryConditional beginsWith(String property, String substring) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.BEGINS_WITH_TEMPLATE, property, just(stringValue(substring)));
    }

    public static QueryConditional beginsWith(String property, Supplier<AttributeValue> value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.BEGINS_WITH_TEMPLATE, property, value);
    }

    public static QueryConditional beginsWith(String property, AttributeValue value) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.BEGINS_WITH_TEMPLATE, property, just(value));
    }

    public static QueryConditional attributeType(String property, AttributeValueType type) {
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue(type.name())));
    }

    public static QueryConditional attributeType(String property, String type) {
        if (ConditionalWithTwoArguments.TYPES.stream().noneMatch(t -> t.equals(type))) {
            throw new IllegalArgumentException("Unrecognized type: " + type);
        }
        return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue(type)));
    }

    public static QueryConditional attributeTypeIsNull(String property) {
        return attributeType(property, (EnhancedType<?>) null);
    }

    public static QueryConditional attributeType(String property, Class<?> type) {
        return attributeType(property, EnhancedType.of(type));
    }

    public static QueryConditional attributeType(String property, EnhancedType<?> type) {
        if (type == null) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("NULL")));
        }

        if (CharSequence.class.isAssignableFrom(type.rawClass())) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("S")));
        }

        if (Number.class.isAssignableFrom(type.rawClass())) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("N")));
        }

        if (Boolean.class.isAssignableFrom(type.rawClass())) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("BOOL")));
        }

        if (SdkBytes.class.isAssignableFrom(type.rawClass())) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("B")));
        }

        if (Map.class.isAssignableFrom(type.rawClass())) {
            return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("B")));
        }

        if (Iterable.class.isAssignableFrom(type.rawClass()) && type.rawClassParameters().size() > 0) {
            Class<?> rawClassParameter = type.rawClassParameters().get(0).rawClass();
            if (CharSequence.class.isAssignableFrom(rawClassParameter)) {
                return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("SS")));
            }

            if (Number.class.isAssignableFrom(rawClassParameter)) {
                return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("NS")));
            }

            if (SdkBytes.class.isAssignableFrom(rawClassParameter)) {
                return new ConditionalWithTwoArguments(ConditionalWithTwoArguments.ATTRIBUTE_TYPE_TEMPLATE, property, just(stringValue("BS")));
            }
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

    private static class Just implements Supplier<AttributeValue> {

        private final AttributeValue value;

        public Just(AttributeValue value) {
            this.value = value;
        }

        @Override
        public AttributeValue get() {
            return value;
        }
    }

    private static <T> AttributeValue convert(AttributeConverterProvider converterProvider, T value) {
        if (value == null) {
            return AttributeValues.nullAttributeValue();
        }
        return converterProvider.converterFor(EnhancedType.of((Class<T>) value.getClass())).transformFrom(value);
    }

    private static <T> Supplier<AttributeValue> converter(AttributeConverterProvider converterProvider, T value) {
        if (value == null) {
            return AttributeValues::nullAttributeValue;
        }
        return just(converterProvider.converterFor(EnhancedType.of((Class<T>) value.getClass())).transformFrom(value));
    }

    private static <T> Supplier<AttributeValue> just(AttributeValue value) {
        return new Just(value);
    }
}
