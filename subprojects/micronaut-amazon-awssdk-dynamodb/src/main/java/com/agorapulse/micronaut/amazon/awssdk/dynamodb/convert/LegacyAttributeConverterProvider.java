package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Attribute converter which supports some legacy data types such as {@link java.util.Date}.
 */
public class LegacyAttributeConverterProvider implements AttributeConverterProvider {

    private final List<AttributeConverter<?>> customConverters = Arrays.asList(
        new DateToStringAttributeConverter()
    );

    private final Map<EnhancedType<?>, AttributeConverter<?>> customConvertersMap;
    private final AttributeConverterProvider defaultProvider = DefaultAttributeConverterProvider.create();

    public LegacyAttributeConverterProvider() {
        customConvertersMap = customConverters.stream().collect(Collectors.toMap(
            AttributeConverter::type,
            c -> c
        ));
    }

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        return (AttributeConverter<T>) customConvertersMap.computeIfAbsent(enhancedType, defaultProvider::converterFor);
    }

}
