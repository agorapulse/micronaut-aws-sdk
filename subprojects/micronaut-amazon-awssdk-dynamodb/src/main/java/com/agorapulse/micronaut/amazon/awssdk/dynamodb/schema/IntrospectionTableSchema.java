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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.ConvertedJsonAttributeConverter;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.LegacyAttributeConverterProvider;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationMetadataProvider;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedTypeDocumentConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPreserveEmptyObject;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

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
import java.util.Objects;
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
 *   <li>Are annotated with {@link io.micronaut.core.annotation.Introspected} with <code>builder</code> settings</li>
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

    private static final Logger LOGGER = LoggerFactory.getLogger(IntrospectionTableSchema.class);

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
            return ImmutableBeanIntrospectionTableSchema.create(beanClass, context, metaTableSchemaCache);
        } else {
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
            return recursiveCreate(
                beanClass, metaTableSchemaCache, () -> ImmutableBeanIntrospectionTableSchema.create(beanClass, context, metaTableSchemaCache)
            );
        } else {
            return recursiveCreate(
                beanClass, metaTableSchemaCache, () -> BeanIntrospectionTableSchema.create(beanClass, context, metaTableSchemaCache)
            );
        }
    }


    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> immutableClass, MetaTableSchemaCache metaTableSchemaCache, Supplier<TableSchema<T>> tableSchemaSupplier) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(immutableClass);

        // If we get a cache hit...
        if (metaTableSchema.isPresent()) {
            // Either: use the cached concrete TableSchema if we have one
            if (metaTableSchema.get().isInitialized()) {
                return metaTableSchema.get().concreteTableSchema();
            }

            // Or: return the uninitialized MetaTableSchema as this must be a recursive reference, and it will be
            // initialized later as the chain completes
            return metaTableSchema.get();
        }

        return tableSchemaSupplier.get();
    }

    /**
     * Determines whether a class should be treated as immutable for DynamoDB schema generation.
     *
     * @param clazz The class to check
     * @return true if the class is immutable, false otherwise
     */
    static boolean isImmutableClass(Class<?> clazz) {
        // Check if the class has builder support via @Introspected annotation
        var introspectionOptional = BeanIntrospector.SHARED.findIntrospection(clazz);

        if (introspectionOptional.isPresent()) {
            BeanIntrospection<?> introspection = introspectionOptional.get();

            // classes that are not records but have a builder are considered immutable only if they have the annotation present
            if (introspection.getAnnotationMetadata().isAnnotationPresent(Immutable.class) || introspection.getAnnotationMetadata().isAnnotationPresent(DynamoDbImmutable.class)) {
                if (!introspection.hasBuilder()) {
                    LOGGER.error("Class {} is annotated with @Immutable or @DynamoDbImmutable but has no builder. Such classes cannot be used as immutable DynamoDB entities.", clazz);
                }

                // we return true under any circumstance here because the exception will be thrown later
                return true;
            }

            if (clazz.isRecord() && !introspection.hasBuilder()) {
                LOGGER.error("Class {} is a record but has no builder. Records with no builder cannot be used as immutable DynamoDB entities.", clazz);
            }

            return clazz.isRecord();
        }

        return clazz.isRecord();
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
        BeanContext beanContext,
        List<AttributeConverterProvider> attributeConverterProviders
    ) {
        if (List.class.equals(type.getType())) {
            EnhancedType<?> enhancedType = convertTypeToEnhancedType(type.getTypeParameters()[0], metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);
            return (EnhancedType<T>) EnhancedType.listOf(enhancedType);
        } else if (Set.class.equals(type.getType())) {
            EnhancedType<?> enhancedType = convertTypeToEnhancedType(type.getTypeParameters()[0], metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);
            return (EnhancedType<T>) EnhancedType.setOf(enhancedType);
        } else if (Map.class.equals(type.getType())) {
            EnhancedType<?> keyType = convertTypeToEnhancedType(type.getTypeVariable("K").orElseThrow(() -> new IllegalArgumentException("Missing key type")), metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);
            EnhancedType<?> valueType = convertTypeToEnhancedType(type.getTypeVariable("V").orElseThrow(() -> new IllegalArgumentException("Missing value type")), metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);
            return (EnhancedType<T>) EnhancedType.mapOf(keyType, valueType);
        }

        Class<T> clazz = type.getType();
        EnhancedType<T> enhancedType = EnhancedType.of(clazz);

        if (clazz.getPackage() == null || clazz.getPackage().getName().startsWith("java.")) {
            return enhancedType;
        }

        // if there is a converter for the type, we don't need to do anything special
        if (CollectionUtils.isNotEmpty(attributeConverterProviders)) {
            Optional<AttributeConverter<T>> converter = attributeConverterProviders.stream()
                .map(p -> {
                    try {
                        return p.converterFor(enhancedType);
                    } catch (IllegalStateException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
            if (converter.isPresent()) {
                return enhancedType;
            }
        }

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

        return enhancedType;
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

    static <T> List<StaticAttributeTag> collectTagsToAttribute(BeanProperty<T, ?> propertyDescriptor) {
        List<StaticAttributeTag> tags = new java.util.ArrayList<>();
        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.UPDATE_BEHAVIOUR_ANNOTATIONS)
            .flatMap(anno -> anno.enumValue(Enum.class))
            .ifPresent(behavior ->  tags.add(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.updateBehavior(software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.valueOf(behavior.name()))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.PARTITION_KEYS_ANNOTATIONS)
            .ifPresent(anno -> tags.add(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SORT_KEYS_ANNOTATIONS)
            .ifPresent(anno -> tags.add(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_PARTITION_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> tags.add(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey(Arrays.asList(indexNames))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_SORT_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> tags.add(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey(Arrays.asList(indexNames))));
        return tags;
    }

    private IntrospectionTableSchema() {
        // Utility class, no instantiation
    }
}
