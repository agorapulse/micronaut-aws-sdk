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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Immutable;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.ConvertedJsonAttributeConverter;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.LegacyAttributeConverterProvider;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationMetadataProvider;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedTypeDocumentConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static software.amazon.awssdk.enhanced.dynamodb.internal.DynamoDbEnhancedLogger.BEAN_LOGGER;

/**
 * Facade class for generating table schemas using Micronaut bean introspection.
 * Automatically determines whether to use mutable or immutable table schema based on
 * class annotations and structure.
 *
 * <p>
 * This class acts as an entry point for table schema generation and contains shared
 * utility methods used by both {@link BeanIntrospectionTableSchema} and
 * {@link ImmutableBeanIntrospectionTableSchema}.
 * </p>
 *
 * <p>
 * Classes are considered immutable if they:
 * <ul>
 *   <li>Are annotated with {@link DynamoDbImmutable}</li>
 *   <li>Are annotated with {@link Immutable}</li>
 *   <li>Are Java records with a static builder() method</li>
 * </ul>
 * </p>
 *
 * @see BeanIntrospectionTableSchema
 * @see ImmutableBeanIntrospectionTableSchema
 */
public class IntrospectionTableSchema {

    @SuppressWarnings("deprecation")
    static final List<String> PARTITION_KEYS_ANNOTATIONS = Arrays.asList(
        PartitionKey.class.getName(),
        HashKey.class.getName(),
        DynamoDbPartitionKey.class.getName(),
        "com.agorapulse.micronaut.aws.dynamodb.annotation.HashKey"
    );

    @SuppressWarnings("deprecation")
    static final List<String> SORT_KEYS_ANNOTATIONS = Arrays.asList(
        SortKey.class.getName(),
        RangeKey.class.getName(),
        DynamoDbSortKey.class.getName(),
        "com.agorapulse.micronaut.aws.dynamodb.annotation.RangeKey"
    );

    static final List<String> SECONDARY_PARTITION_KEYS_ANNOTATIONS = Arrays.asList(
        SecondaryPartitionKey.class.getName(),
        DynamoDbSecondaryPartitionKey.class.getName()
    );

    static final List<String> SECONDARY_SORT_KEYS_ANNOTATIONS = Arrays.asList(
        SecondarySortKey.class.getName(),
        DynamoDbSecondarySortKey.class.getName()
    );

    static final List<String> UPDATE_BEHAVIOUR_ANNOTATIONS = Arrays.asList(
        UpdateBehavior.class.getName(),
        DynamoDbUpdateBehavior.class.getName()
    );

    /**
     * Creates a table schema for the given class using Micronaut bean introspection.
     * Automatically determines whether to create a mutable or immutable table schema
     * based on the class structure and annotations.
     *
     * @param <T> The type of object that this {@link TableSchema} maps to.
     * @param beanClass The class to create a table schema for
     * @param context The Micronaut bean context
     * @param metaTableSchemaCache Cache for table schemas to prevent infinite recursion
     * @return A table schema for the given class
     */
    public static <T> TableSchema<T> create(Class<T> beanClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        if (isImmutableClass(beanClass)) {
            // For immutable classes, determine the builder class and create immutable schema
            @SuppressWarnings("unchecked")
            Class<Object> builderClass = (Class<Object>) determineBuilderClass(beanClass);
            return ImmutableBeanIntrospectionTableSchema.create(beanClass, builderClass, context, metaTableSchemaCache);
        } else {
            // For mutable classes, use the standard bean schema
            return BeanIntrospectionTableSchema.create(beanClass, context, metaTableSchemaCache);
        }
    }

    /**
     * Creates a table schema recursively, utilizing the MetaTableSchema cache to prevent infinite recursion.
     * This method is used when creating nested schemas for document types.
     *
     * @param <T> The type of object that this {@link TableSchema} maps to.
     * @param beanClass The class to create a table schema for
     * @param context The Micronaut bean context
     * @param metaTableSchemaCache Cache for table schemas to prevent infinite recursion
     * @return A table schema for the given class
     */
    static <T> TableSchema<T> recursiveCreate(Class<T> beanClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        if (isImmutableClass(beanClass)) {
            return ImmutableBeanIntrospectionTableSchema.recursiveCreate(beanClass, context, metaTableSchemaCache);
        } else {
            return BeanIntrospectionTableSchema.recursiveCreate(beanClass, context, metaTableSchemaCache);
        }
    }

    /**
     * Determines whether a class should be treated as immutable for DynamoDB schema generation.
     *
     * @param clazz The class to check
     * @return true if the class is immutable, false otherwise
     */
    static boolean isImmutableClass(Class<?> clazz) {
        // Check for explicit immutable annotations
        if (clazz.getAnnotation(DynamoDbImmutable.class) != null) {
            return true;
        }
        if (clazz.getAnnotation(Immutable.class) != null) {
            return true;
        }

        // Check if it's a record with a static builder() method
        if (clazz.isRecord()) {
            // using var to prevent generics issue
            var introspectionOptional = BeanIntrospector.SHARED.findIntrospection(clazz);
            if (introspectionOptional.isPresent()) {
                BeanIntrospection<?> introspection = introspectionOptional.get();
                return introspection.getBeanMethods().stream()
                    .anyMatch(method -> "builder".equals(method.getName()) && method.getArguments().length == 0);
            }
        }

        return false;
    }

    /**
     * Determines the builder class for an immutable class.
     * For classes annotated with @DynamoDbImmutable or @Immutable, uses the annotation's builder value.
     * For records with a static builder() method, uses the return type of that method.
     * Otherwise, falls back to looking for a Builder inner class.
     *
     * @param <T> The immutable class type
     * @param immutableClass The immutable class
     * @return The builder class for the immutable class
     * @throws IllegalArgumentException if no builder class can be determined
     */
    static <T> Class<?> determineBuilderClass(Class<T> immutableClass) {
        // First check for @DynamoDbImmutable annotation
        DynamoDbImmutable immutableAnnotation = immutableClass.getAnnotation(DynamoDbImmutable.class);
        if (immutableAnnotation != null) {
            return immutableAnnotation.builder();
        }

        // Then check for our @Immutable annotation alias
        Immutable immutableAlias = immutableClass.getAnnotation(Immutable.class);
        if (immutableAlias != null) {
            return immutableAlias.builder();
        }

        // For records, check if there's a static builder() method
        if (immutableClass.isRecord()) {
            Optional<BeanIntrospection<T>> introspectionOptional = BeanIntrospector.SHARED.findIntrospection(immutableClass);
            if (introspectionOptional.isPresent()) {
                BeanIntrospection<T> introspection = introspectionOptional.get();
                return introspection.getBeanMethods().stream()
                    .filter(method -> "builder".equals(method.getName()))
                    .filter(method -> method.getArguments().length == 0)
                    // For records with @Introspected, static methods should be included in getBeanMethods()
                    // We assume that a parameterless builder() method is static (common record pattern)
                    .map(method -> method.getReturnType().getType())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Record " + immutableClass.getTypeName() + " must have a static builder() method"));
            }
        }

        // Fall back to looking for Builder inner class (common convention)
        try {
            return Class.forName(immutableClass.getName() + "$Builder");
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot determine builder class for " + immutableClass.getTypeName() +
                ". Please ensure the class is annotated with @DynamoDbImmutable, is a record with a static builder() method, or has a Builder inner class.", e);
        }
    }

    /**
     * Resolves attribute configuration from property annotations.
     *
     * @param <T> The bean type
     * @param propertyDescriptor The property to resolve configuration for
     * @return The attribute configuration
     */
    static <T> AttributeConfiguration resolveAttributeConfiguration(BeanProperty<T, ?> propertyDescriptor) {
        boolean shouldPreserveEmptyObject = findAnnotation(propertyDescriptor, DynamoDbPreserveEmptyObject.class, PreserveEmptyObjects.class).isPresent();
        boolean shouldIgnoreNulls = findAnnotation(propertyDescriptor, DynamoDbIgnoreNulls.class, IgnoreNulls.class).isPresent();

        return AttributeConfiguration.builder()
            .preserveEmptyObject(shouldPreserveEmptyObject)
            .ignoreNulls(shouldIgnoreNulls)
            .build();
    }

    /**
     * Creates converter providers from @DynamoDbBean annotation.
     *
     * @param dynamoDbBean The @DynamoDbBean annotation
     * @param beanContext The Micronaut bean context
     * @return List of attribute converter providers
     */
    @SuppressWarnings("unchecked")
    static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(AnnotationValue<?> dynamoDbBean, BeanContext beanContext) {
        Class<? extends AttributeConverterProvider>[] providerClasses = (Class<? extends AttributeConverterProvider>[]) dynamoDbBean.classValues("converterProviders");
        if (providerClasses.length == 0) {
            providerClasses = new Class[]{LegacyAttributeConverterProvider.class};
        }

        return Arrays.stream(providerClasses).map(c -> (AttributeConverterProvider) fromContextOrNew(c, beanContext).get()).toList();
    }

    /**
     * Converts a {@link Argument} to an {@link EnhancedType}. Usually {@link EnhancedType#of} is capable of doing this all
     * by itself, but for the table schemas we want to detect if a parameterized class is being passed without a
     * converter that is actually another annotated class in which case we want to capture its schema and add it to the
     * EnhancedType. Unfortunately this means we have to duplicate some of the recursive Type parsing that
     * EnhancedClient otherwise does all by itself.
     */
    @SuppressWarnings("unchecked")
    static <T> EnhancedType<T> convertTypeToEnhancedType(
        Argument<T> type,
        MetaTableSchemaCache metaTableSchemaCache,
        AttributeConfiguration attributeConfiguration,
        BeanContext beanContext
    ) {
        if (List.class.equals(type.getType())) {
            EnhancedType<?> enhancedType = convertTypeToEnhancedType(type.getTypeParameters()[0], metaTableSchemaCache, attributeConfiguration, beanContext);
            return (EnhancedType<T>) EnhancedType.listOf(enhancedType);
        } else if (Set.class.equals(type.getType())) {
            EnhancedType<?> enhancedType = convertTypeToEnhancedType(type.getTypeParameters()[0], metaTableSchemaCache, attributeConfiguration, beanContext);
            return (EnhancedType<T>) EnhancedType.setOf(enhancedType);
        } else if (Map.class.equals(type.getType())) {
            EnhancedType<?> keyType = convertTypeToEnhancedType(type.getTypeVariable("K").orElseThrow(() -> new IllegalArgumentException("Missing key type")), metaTableSchemaCache, attributeConfiguration, beanContext);
            EnhancedType<?> valueType = convertTypeToEnhancedType(type.getTypeVariable("V").orElseThrow(() -> new IllegalArgumentException("Missing value type")), metaTableSchemaCache, attributeConfiguration, beanContext);
            return (EnhancedType<T>) EnhancedType.mapOf(keyType, valueType);
        }

        Class<T> clazz = type.getType();

        if (clazz.getPackage() != null && !clazz.getPackage().getName().startsWith("java.")) {
            Optional<BeanIntrospection<T>> introspection = BeanIntrospector.SHARED.findIntrospection(clazz);
            if (introspection.isPresent()) {
                Consumer<EnhancedTypeDocumentConfiguration.Builder> attrConfiguration = b -> b
                    .preserveEmptyObject(attributeConfiguration.preserveEmptyObject())
                    .ignoreNulls(attributeConfiguration.ignoreNulls());

                // Use the TableSchemaGenerator to recursively create schemas based on immutability
                return EnhancedType.documentOf(
                    clazz,
                    recursiveCreate(clazz, beanContext, metaTableSchemaCache),
                    attrConfiguration
                );
            }
        }

        return EnhancedType.of(clazz);
    }

    /**
     * Creates an attribute converter from property annotations.
     *
     * @param <T> The bean type
     * @param <P> The property type
     * @param propertyDescriptor The property descriptor
     * @param beanContext The Micronaut bean context
     * @return An optional attribute converter
     */
    @SuppressWarnings("unchecked")
    static <T, P> Optional<AttributeConverter<P>> createAttributeConverterFromAnnotation(
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext
    ) {
        return findAnnotation(propertyDescriptor, DynamoDbConvertedBy.class, ConvertedBy.class)
            .flatMap(AnnotationValue::classValue)
            .map(clazz -> (AttributeConverter<P>) fromContextOrNew(clazz, beanContext).get())
            .or(() -> findAnnotation(propertyDescriptor, ConvertedJson.class)
                .map(anno -> (AttributeConverter<P>) new ConvertedJsonAttributeConverter<>(propertyDescriptor.getType())));
    }

    /**
     * Creates an instant getter function from a property for TTL functionality.
     *
     * @param <T> The bean type
     * @param ttl The TTL annotation
     * @param property The property to create getter for
     * @param beanContext The Micronaut bean context
     * @return A function that extracts an Instant from the property
     */
    static <T> Function<T, Instant> createInstantGetter(AnnotationValue<TimeToLive> ttl, BeanProperty<T, ?> property, BeanContext beanContext) {
        if (Instant.class.isAssignableFrom(property.getType())) {
            return instance -> (Instant) property.get(instance);
        }

        if (CharSequence.class.isAssignableFrom(property.getType())) {
            DateTimeFormatter formatter = ttl
                .stringValue("format")
                .filter(StringUtils::isNotEmpty)
                .map(DateTimeFormatter::ofPattern)
                .orElse(DateTimeFormatter.ISO_INSTANT);

            return instance -> {
                try {
                    TemporalAccessor parsed = formatter.parse((CharSequence) property.get(instance));
                    if (!parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
                        if (parsed.isSupported(ChronoField.HOUR_OF_DAY)) {
                            return LocalDateTime.from(parsed).atZone(ZoneOffset.UTC).toInstant();
                        }
                        return LocalDate.from(parsed).atStartOfDay(ZoneOffset.UTC).toInstant();
                    }
                    return Instant.from(parsed);
                } catch (DateTimeParseException e) {
                    throw new TimeToLiveExtractionFailedException(instance, property, "Failed to extract TTL from property %s of instance %s".formatted(property.getName(), instance), e);
                }
            };
        }

        if (Number.class.isAssignableFrom(property.getType())) {
            return instance -> Instant.ofEpochMilli(((Number) property.get(instance)).longValue());
        }

        if (beanContext.getConversionService().canConvert(property.getType(), Instant.class)) {
            return instance -> beanContext.getConversionService().convert(property.get(instance), Instant.class).orElseThrow(
                () -> new TimeToLiveExtractionFailedException(instance, property, "Failed to convert " + property.get(instance) + " to Instant for field " + property + " annotated with @TimeToLive " + ttl, null)
            );
        }

        throw new IllegalArgumentException("TimeToLive annotation can only be used on fields of type Instant, String or Long or any type that can be converted using ConversionService but was used on " + property);
    }

    /**
     * Creates a supplier for a class from the Micronaut bean context or by instantiation.
     *
     * @param <R> The class type
     * @param clazz The class to create supplier for
     * @param beanContext The Micronaut bean context
     * @return A supplier for the class
     */
    static <R> Supplier<R> fromContextOrNew(Class<R> clazz, BeanContext beanContext) {
        Optional<R> optionalBean = beanContext.findBean(clazz);
        if (optionalBean.isPresent()) {
            return optionalBean::get;
        }

        Optional<BeanIntrospection<R>> optionalBeanIntrospection = BeanIntrospector.SHARED.findIntrospection(clazz);
        if (optionalBeanIntrospection.isPresent()) {
            return optionalBeanIntrospection.get()::instantiate;
        }

        try {
            Constructor<R> constructor = clazz.getConstructor();
            debugLog(clazz, () -> "Constructor: " + constructor);
            return ObjectConstructor.create(clazz, constructor, MethodHandles.lookup());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("Class '%s' appears to have no default constructor thus cannot be used with table schema", clazz), e);
        }
    }

    /**
     * Determines the DynamoDB attribute name for a property.
     *
     * @param <T> The bean type
     * @param propertyDescriptor The property descriptor
     * @return The attribute name
     */
    static <T> String attributeNameForProperty(BeanProperty<T, ?> propertyDescriptor) {
        return findAnnotation(propertyDescriptor, DynamoDbAttribute.class, Attribute.class)
            .flatMap(AnnotationValue::stringValue)
            .orElseGet(propertyDescriptor::getName);
    }

    /**
     * Logs debug messages for table schema generation.
     *
     * @param beanClass The class being processed
     * @param logMessage Supplier for the log message
     */
    static void debugLog(Class<?> beanClass, Supplier<String> logMessage) {
        BEAN_LOGGER.debug(() -> beanClass.getTypeName() + " - " + logMessage.get());
    }

    /**
     * Finds an annotation on a property from a list of annotation classes.
     *
     * @param source The annotation metadata provider
     * @param annotationClasses The annotation classes to search for
     * @return An optional annotation value
     */
    @SafeVarargs
    static Optional<AnnotationValue<Annotation>> findAnnotation(AnnotationMetadataProvider source, Class<? extends Annotation>... annotationClasses) {
        for (Class<? extends Annotation> annotationName : annotationClasses) {
            @SuppressWarnings("unchecked")
            Optional<AnnotationValue<Annotation>> maybeAnno = source.findAnnotation((Class<Annotation>) annotationName);
            if (maybeAnno.isPresent()) {
                return maybeAnno;
            }
        }
        return Optional.empty();
    }

    /**
     * Finds an annotation on a property from a list of annotation names.
     *
     * @param source The annotation metadata provider
     * @param annotationNames The annotation names to search for
     * @return An optional annotation value
     */
    static Optional<AnnotationValue<Annotation>> findAnnotation(AnnotationMetadataProvider source, Iterable<String> annotationNames) {
        for (String annotationName : annotationNames) {
            Optional<AnnotationValue<Annotation>> maybeAnno = source.findAnnotation(annotationName);
            if (maybeAnno.isPresent()) {
                return maybeAnno;
            }
        }
        return Optional.empty();
    }


    static <T> boolean isNotIgnored(Class<T> beanClass, BeanProperty<T, ?> propertyDescriptor) {
        if (propertyDescriptor.isAnnotationPresent(DynamoDbIgnore.class)) {
            IntrospectionTableSchema.debugLog(beanClass, () -> "Ignoring bean property %s because it is ignored.".formatted(propertyDescriptor.getName()));
            return false;
        }

        if (propertyDescriptor.isAnnotationPresent(Ignore.class)) {
            IntrospectionTableSchema.debugLog(beanClass, () -> "Ignoring bean property %s because it is ignored.".formatted(propertyDescriptor.getName()));
            return false;
        }

        return true;
    }

    private IntrospectionTableSchema() {
        // Utility class, no instantiation
    }
}
