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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedTypeDocumentConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.ObjectConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.WrappedTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static software.amazon.awssdk.enhanced.dynamodb.internal.DynamoDbEnhancedLogger.BEAN_LOGGER;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of an immutable
 * class using Micronaut bean introspection instead of reflection. This is based on AWS SDK's ImmutableTableSchema
 * but adapted to work with Micronaut's introspection capabilities.
 * 
 * Supports traditional immutable classes with {@link DynamoDbImmutable} annotation, classes with the more 
 * concise {@link Immutable} annotation, and Java records with a static builder() method.
 * 
 * Example with @DynamoDbImmutable annotation:
 * <pre>
 * <code>
 * {@literal @}Introspected
 * {@literal @}DynamoDbImmutable(builder = Customer.Builder.class)
 * public class Customer {
 *     {@literal @}DynamoDbPartitionKey
 *     public String accountId() { ... }
 *
 *     {@literal @}DynamoDbSortKey
 *     public int subId() { ... }
 *
 *     // Builder class (can be inner or separate class)
 *     {@literal @}Introspected
 *     public static final class Builder {
 *         public Builder accountId(String accountId) { ... };
 *         public Builder subId(int subId) { ... };
 *         public Builder name(String name) { ... };
 *         public Builder createdDate(Instant createdDate) { ... };
 *
 *         public Customer build() { ... };
 *     }
 * }
 * </pre>
 * 
 * Example with @Immutable annotation (more concise):
 * <pre>
 * <code>
 * {@literal @}Introspected
 * {@literal @}Immutable(builder = Customer.Builder.class)
 * public class Customer {
 *     {@literal @}DynamoDbPartitionKey
 *     public String accountId() { ... }
 *
 *     {@literal @}DynamoDbSortKey
 *     public int subId() { ... }
 *
 *     // Builder class (can be inner or separate class)
 *     {@literal @}Introspected
 *     public static final class Builder {
 *         public Builder accountId(String accountId) { ... };
 *         public Builder subId(int subId) { ... };
 *         public Builder name(String name) { ... };
 *         public Builder createdDate(Instant createdDate) { ... };
 *
 *         public Customer build() { ... };
 *     }
 * }
 * </pre>
 * 
 * Example with Java record (no annotation required):
 * <pre>
 * <code>
 * {@literal @}Introspected
 * public record Customer(
 *     {@literal @}DynamoDbPartitionKey String accountId,
 *     {@literal @}DynamoDbSortKey int subId,
 *     {@literal @}DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name") String name,
 *     {@literal @}DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"}) Instant createdDate
 * ) {
 *     public static Builder builder() {
 *         return new Builder();
 *     }
 *     
 *     {@literal @}Introspected
 *     public static class Builder {
 *         // builder implementation
 *         public Customer build() { ... }
 *     }
 * }
 * </pre>
 * <p>
 * Creating an {@link ImmutableBeanIntrospectionTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
 * usually done once at application startup.
 * <p>
 *
 * @param <T> The type of object that this {@link TableSchema} maps to.
 * @param <B> The type of the builder class used to construct instances of T.
 */
@SdkPublicApi
@ThreadSafe
public final class ImmutableBeanIntrospectionTableSchema<T, B> extends WrappedTableSchema<T, StaticImmutableTableSchema<T, B>> {

    @SuppressWarnings("deprecation")
    private static final List<String> PARTITION_KEYS_ANNOTATIONS = Arrays.asList(
        PartitionKey.class.getName(),
        HashKey.class.getName(),
        DynamoDbPartitionKey.class.getName(),
        "com.agorapulse.micronaut.aws.dynamodb.annotation.HashKey"
    );

    @SuppressWarnings("deprecation")
    private static final List<String> SORT_KEYS_ANNOTATIONS = Arrays.asList(
        SortKey.class.getName(),
        RangeKey.class.getName(),
        DynamoDbSortKey.class.getName(),
        "com.agorapulse.micronaut.aws.dynamodb.annotation.RangeKey"
    );

    private static final List<String> SECONDARY_PARTITION_KEYS_ANNOTATIONS = Arrays.asList(
        SecondaryPartitionKey.class.getName(),
        DynamoDbSecondaryPartitionKey.class.getName()
    );

    private static final List<String> SECONDARY_SORT_KEYS_ANNOTATIONS = Arrays.asList(
        SecondarySortKey.class.getName(),
        DynamoDbSecondarySortKey.class.getName()
    );
    
    private static final List<String> UPDATE_BEHAVIOUR_ANNOTATIONS = Arrays.asList(
        UpdateBehavior.class.getName(),
        DynamoDbUpdateBehavior.class.getName()
    );

    private static final String ATTRIBUTE_TAG_STATIC_SUPPLIER_NAME = "attributeTagFor";

    private ImmutableBeanIntrospectionTableSchema(StaticImmutableTableSchema<T, B> staticImmutableTableSchema) {
        super(staticImmutableTableSchema);
    }

    public static <T, B> ImmutableBeanIntrospectionTableSchema<T, B> create(Class<T> immutableClass, Class<B> builderClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        debugLog(immutableClass, () -> "Creating immutable bean introspection schema");
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(immutableClass);

        ImmutableBeanIntrospectionTableSchema<T, B> newTableSchema =
            new ImmutableBeanIntrospectionTableSchema<>(createStaticImmutableTableSchema(immutableClass, builderClass, context, metaTableSchemaCache));
        if (!metaTableSchema.isInitialized()) {
            metaTableSchema.initialize(newTableSchema);
        }
        return newTableSchema;
    }

    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> immutableClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(immutableClass);

        // If we get a cache hit...
        if (metaTableSchema.isPresent()) {
            // Either: use the cached concrete TableSchema if we have one
            if (metaTableSchema.get().isInitialized()) {
                return metaTableSchema.get().concreteTableSchema();
            }

            // Or: return the uninitialized MetaTableSchema as this must be a recursive reference and it will be
            // initialized later as the chain completes
            return metaTableSchema.get();
        }

        // Otherwise: cache doesn't know about this class; create a new one from scratch
        @SuppressWarnings("unchecked")
        Class<Object> builderClass = (Class<Object>) determineBuilderClass(immutableClass);
        
        return create(immutableClass, builderClass, context, metaTableSchemaCache);
    }

    /**
     * Determines the builder class for an immutable class.
     * For classes annotated with @DynamoDbImmutable or @Immutable, uses the annotation's builder value.
     * For records with a static builder() method, uses the return type of that method.
     * Otherwise, falls back to looking for a Builder inner class.
     */
    private static <T> Class<?> determineBuilderClass(Class<T> immutableClass) {
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

    private static <T, B> StaticImmutableTableSchema<T, B> createStaticImmutableTableSchema(
        Class<T> immutableClass,
        Class<B> builderClass,
        BeanContext beanContext,
        MetaTableSchemaCache metaTableSchemaCache) {

        Optional<BeanIntrospection<T>> immutableIntrospectionOptional = BeanIntrospector.SHARED.findIntrospection(immutableClass);
        Optional<BeanIntrospection<B>> builderIntrospectionOptional = BeanIntrospector.SHARED.findIntrospection(builderClass);

        if (!immutableIntrospectionOptional.isPresent()) {
            throw new IllegalArgumentException("An immutable DynamoDb class must be annotated with @Introspected, but " + immutableClass.getTypeName() + " was not.");
        }

        if (!builderIntrospectionOptional.isPresent()) {
            throw new IllegalArgumentException("An immutable DynamoDb builder class must be annotated with @Introspected, but " + builderClass.getTypeName() + " was not.");
        }

        BeanIntrospection<T> immutableIntrospection = immutableIntrospectionOptional.get();
        BeanIntrospection<B> builderIntrospection = builderIntrospectionOptional.get();

        // Find the build method in the builder
        BeanProperty<B, T> buildMethod = findBuildMethod(builderIntrospection, immutableClass);
        
        Supplier<B> newBuilderSupplier = builderIntrospection::instantiate;
        Function<B, T> buildFunction = createBuildFunction(buildMethod);

        StaticImmutableTableSchema.Builder<T, B> builder = StaticImmutableTableSchema.builder(immutableClass, builderClass)
            .newItemBuilder(newBuilderSupplier, buildFunction);

        Optional<AnnotationValue<DynamoDbBean>> optionalDynamoDbBean = immutableIntrospection.findAnnotation(DynamoDbBean.class);
        Optional<AnnotationValue<DynamoDbImmutable>> optionalDynamoDbImmutable = immutableIntrospection.findAnnotation(DynamoDbImmutable.class);
        Optional<AnnotationValue<Immutable>> optionalImmutable = immutableIntrospection.findAnnotation(Immutable.class);
        
        if (optionalDynamoDbBean.isPresent()) {
            builder.attributeConverterProviders(createConverterProvidersFromAnnotation(immutableClass, optionalDynamoDbBean.get(), beanContext));
        } else if (optionalDynamoDbImmutable.isPresent()) {
            builder.attributeConverterProviders(createConverterProvidersFromDynamoDbImmutableAnnotation(immutableClass, optionalDynamoDbImmutable.get(), beanContext));
        } else if (optionalImmutable.isPresent()) {
            builder.attributeConverterProviders(createConverterProvidersFromImmutableAnnotation(immutableClass, optionalImmutable.get(), beanContext));
        } else {
            builder.attributeConverterProviders(new LegacyAttributeConverterProvider());
        }

        List<ImmutableAttribute<T, B, ?>> attributes = new ArrayList<>();

        immutableIntrospection.getBeanProperties().stream()
            .filter(p -> isMappableProperty(immutableClass, p))
            .map(propertyDescriptor -> {
                propertyDescriptor.findAnnotation(TimeToLive.class).ifPresent(timeToLive ->
                    attributes.add(createTtlAttributeFromFieldAnnotation(immutableClass, builderClass, timeToLive, propertyDescriptor, beanContext))
                );
                return extractAttributeFromProperty(immutableClass, builderClass, builderIntrospection, metaTableSchemaCache, builder, propertyDescriptor, beanContext);
            })
            .filter(Objects::nonNull)
            .forEach(attributes::add);

        immutableIntrospection.findAnnotation(TimeToLive.class).ifPresent(ttl -> 
            attributes.add(createTtlAttributeFromTopLevelAnnotation(immutableClass, builderClass, ttl, beanContext)));

        builder.attributes(attributes);

        return builder.build();
    }

    private static <B, T> BeanProperty<B, T> findBuildMethod(BeanIntrospection<B> builderIntrospection, Class<T> immutableClass) {
        // Look for a method property that returns the immutable type
        return builderIntrospection.getBeanMethods().stream()
            .filter(method -> immutableClass.isAssignableFrom(method.getReturnType().getType()))
            .filter(method -> method.getArguments().length == 0)
            .map(method -> (BeanProperty<B, T>) method)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Builder class " + builderIntrospection.getBeanType().getTypeName() + " must have a parameterless method that returns " + immutableClass.getTypeName()));
    }

    private static <B, T> Function<B, T> createBuildFunction(BeanProperty<B, T> buildMethod) {
        return buildMethod::get;
    }

    private static <T, B> ImmutableAttribute<T, B, ?> createTtlAttributeFromTopLevelAnnotation(
        Class<T> immutableClass, Class<B> builderClass, AnnotationValue<TimeToLive> ttl, BeanContext beanContext) {
        
        long durationInSeconds = beanContext.getConversionService().convertRequired(ttl.getRequiredValue(String.class), Duration.class).getSeconds();
        
        return ImmutableAttribute.builder(immutableClass, builderClass, EnhancedType.of(Long.class))
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> System.currentTimeMillis() / 1000 + durationInSeconds)
            .setter((builder, value) -> { }) // TTL is read-only
            .build();
    }

    private static <T, B> ImmutableAttribute<T, B, ?> createTtlAttributeFromFieldAnnotation(
        Class<T> immutableClass, Class<B> builderClass, AnnotationValue<TimeToLive> ttl, BeanProperty<T, ?> property, BeanContext beanContext) {
        
        Duration duration = beanContext.getConversionService().convertRequired(ttl.getRequiredValue(String.class), Duration.class);
        Function<T, Instant> toInstant = createInstantGetter(ttl, property, beanContext);

        return ImmutableAttribute.builder(immutableClass, builderClass, EnhancedType.of(Long.class))
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> Optional.ofNullable(toInstant.apply(instance)).orElseGet(Instant::now).plus(duration).getEpochSecond())
            .setter((builder, value) -> { }) // TTL is read-only
            .build();
    }

    private static <T> Function<T, Instant> createInstantGetter(AnnotationValue<TimeToLive> ttl, BeanProperty<T, ?> property, BeanContext beanContext) {
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

    private static <T, B, P> ImmutableAttribute<T, B, P> extractAttributeFromProperty(
        Class<T> immutableClass,
        Class<B> builderClass,
        BeanIntrospection<B> builderIntrospection,
        MetaTableSchemaCache metaTableSchemaCache,
        StaticImmutableTableSchema.Builder<T, B> builder,
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext
    ) {
        Optional<AnnotationValue<Annotation>> dynamoDbFlatten = findAnnotation(propertyDescriptor, DynamoDbFlatten.class, Flatten.class);

        if (dynamoDbFlatten.isPresent()) {
            // Find corresponding setter in builder
            BeanProperty<B, P> builderProperty = findBuilderProperty(builderIntrospection, propertyDescriptor.getName());
            
            builder.flatten(
                ImmutableBeanIntrospectionTableSchema.create(propertyDescriptor.getType(), findBuilderClassForProperty(propertyDescriptor.getType()), beanContext, metaTableSchemaCache),
                propertyDescriptor::get,
                builderProperty != null ? builderProperty::set : (builderInstance, value) -> { }
            );
            return null;
        } else {
            AttributeConfiguration attributeConfiguration = resolveAttributeConfiguration(propertyDescriptor);

            ImmutableAttribute.Builder<T, B, P> attributeBuilder = immutableAttributeBuilder(
                propertyDescriptor, builderIntrospection, immutableClass, builderClass, metaTableSchemaCache, attributeConfiguration, beanContext);

            createAttributeConverterFromAnnotation(propertyDescriptor, beanContext).ifPresent(attributeBuilder::attributeConverter);

            addTagsToAttribute(attributeBuilder, propertyDescriptor);
            return attributeBuilder.build();
        }
    }

    private static <T, B, P> ImmutableAttribute.Builder<T, B, P> immutableAttributeBuilder(
        BeanProperty<T, P> propertyDescriptor,
        BeanIntrospection<B> builderIntrospection,
        Class<T> immutableClass,
        Class<B> builderClass,
        MetaTableSchemaCache metaTableSchemaCache,
        AttributeConfiguration attributeConfiguration,
        BeanContext beanContext
    ) {
        Argument<P> propertyType = propertyDescriptor.asArgument();
        EnhancedType<P> propertyTypeToken = convertTypeToEnhancedType(propertyType, metaTableSchemaCache, attributeConfiguration, beanContext);
        
        // Find corresponding setter in builder
        BeanProperty<B, P> builderProperty = findBuilderProperty(builderIntrospection, propertyDescriptor.getName());
        
        return ImmutableAttribute.builder(immutableClass, builderClass, propertyTypeToken)
            .name(attributeNameForProperty(propertyDescriptor))
            .getter(propertyDescriptor::get)
            .setter(builderProperty != null ? builderProperty::set : (builderInstance, value) -> { });
    }

    @SuppressWarnings("unchecked")
    private static <B, P> BeanProperty<B, P> findBuilderProperty(BeanIntrospection<B> builderIntrospection, String propertyName) {
        return (BeanProperty<B, P>) builderIntrospection.getBeanProperties().stream()
            .filter(prop -> prop.getName().equals(propertyName))
            .findFirst()
            .orElse(null);
    }

    private static <T> Class<?> findBuilderClassForProperty(Class<T> propertyClass) {
        return determineBuilderClass(propertyClass);
    }

    private static <T> AttributeConfiguration resolveAttributeConfiguration(BeanProperty<T, ?> propertyDescriptor) {
        boolean shouldPreserveEmptyObject = findAnnotation(propertyDescriptor, DynamoDbPreserveEmptyObject.class, PreserveEmptyObjects.class).isPresent();
        boolean shouldIgnoreNulls = findAnnotation(propertyDescriptor, DynamoDbIgnoreNulls.class, IgnoreNulls.class).isPresent();

        return AttributeConfiguration.builder()
            .preserveEmptyObject(shouldPreserveEmptyObject)
            .ignoreNulls(shouldIgnoreNulls)
            .build();
    }

    @SuppressWarnings("unchecked")
    private static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(Class<?> immutableClass, AnnotationValue<DynamoDbBean> dynamoDbBean, BeanContext beanContext) {
        Class<? extends AttributeConverterProvider>[] providerClasses = (Class<? extends AttributeConverterProvider>[]) dynamoDbBean.classValues("converterProviders");
        if (providerClasses.length == 0) {
            providerClasses = new Class[]{LegacyAttributeConverterProvider.class};
        }

        return Arrays.stream(providerClasses)
            .peek(c -> debugLog(immutableClass, () -> "Adding Converter: " + c.getTypeName()))
            .map(c -> (AttributeConverterProvider) fromContextOrNew(c, beanContext).get())
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static List<AttributeConverterProvider> createConverterProvidersFromDynamoDbImmutableAnnotation(Class<?> immutableClass, AnnotationValue<DynamoDbImmutable> dynamoDbImmutable, BeanContext beanContext) {
        Class<? extends AttributeConverterProvider>[] providerClasses = (Class<? extends AttributeConverterProvider>[]) dynamoDbImmutable.classValues("converterProviders");
        if (providerClasses.length == 0) {
            providerClasses = new Class[]{LegacyAttributeConverterProvider.class};
        }

        return Arrays.stream(providerClasses)
            .peek(c -> debugLog(immutableClass, () -> "Adding Converter: " + c.getTypeName()))
            .map(c -> (AttributeConverterProvider) fromContextOrNew(c, beanContext).get())
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static List<AttributeConverterProvider> createConverterProvidersFromImmutableAnnotation(Class<?> immutableClass, AnnotationValue<Immutable> immutable, BeanContext beanContext) {
        Class<? extends AttributeConverterProvider>[] providerClasses = (Class<? extends AttributeConverterProvider>[]) immutable.classValues("converterProviders");
        if (providerClasses.length == 0) {
            providerClasses = new Class[]{LegacyAttributeConverterProvider.class};
        }

        return Arrays.stream(providerClasses)
            .peek(c -> debugLog(immutableClass, () -> "Adding Converter: " + c.getTypeName()))
            .map(c -> (AttributeConverterProvider) fromContextOrNew(c, beanContext).get())
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> EnhancedType<T> convertTypeToEnhancedType(
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

                // Check if it's an immutable class (has @DynamoDbImmutable, @Immutable, or is a record)
                if (clazz.getAnnotation(DynamoDbImmutable.class) != null || 
                    clazz.getAnnotation(Immutable.class) != null || 
                    clazz.isRecord()) {
                    return EnhancedType.documentOf(
                        clazz,
                        ImmutableBeanIntrospectionTableSchema.recursiveCreate(clazz, beanContext, metaTableSchemaCache),
                        attrConfiguration
                    );
                } else {
                    return EnhancedType.documentOf(
                        clazz,
                        BeanIntrospectionTableSchema.recursiveCreate(clazz, beanContext, metaTableSchemaCache),
                        attrConfiguration
                    );
                }
            }
        }

        return EnhancedType.of(clazz);
    }

    @SuppressWarnings("unchecked")
    private static <T, P> Optional<AttributeConverter<P>> createAttributeConverterFromAnnotation(
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext
    ) {
        return findAnnotation(propertyDescriptor, DynamoDbConvertedBy.class, ConvertedBy.class)
            .flatMap(AnnotationValue::classValue)
            .map(clazz -> (AttributeConverter<P>) fromContextOrNew(clazz, beanContext).get())
            .or(() -> findAnnotation(propertyDescriptor, ConvertedJson.class)
                .map(anno -> (AttributeConverter<P>) new ConvertedJsonAttributeConverter<>(propertyDescriptor.getType())));
    }

    private static <T> void addTagsToAttribute(ImmutableAttribute.Builder<?, ?, ?> attributeBuilder,
                                               BeanProperty<T, ?> propertyDescriptor) {

        findAnnotation(propertyDescriptor, UPDATE_BEHAVIOUR_ANNOTATIONS)
            .flatMap(anno -> anno.enumValue(Enum.class))
            .ifPresent(behavior -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.updateBehavior(software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.valueOf(behavior.name()))));

        findAnnotation(propertyDescriptor, PARTITION_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey()));

        findAnnotation(propertyDescriptor, SORT_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey()));

        findAnnotation(propertyDescriptor, SECONDARY_PARTITION_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey(Arrays.asList(indexNames))));

        findAnnotation(propertyDescriptor, SECONDARY_SORT_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey(Arrays.asList(indexNames))));
    }

    private static <R> Supplier<R> fromContextOrNew(Class<R> clazz, BeanContext beanContext) {
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
                String.format("Class '%s' appears to have no default constructor thus cannot be used with the ImmutableBeanIntrospectionTableSchema", clazz), e);
        }
    }

    private static <T> String attributeNameForProperty(BeanProperty<T, ?> propertyDescriptor) {
        return findAnnotation(propertyDescriptor, DynamoDbAttribute.class, Attribute.class)
            .flatMap(AnnotationValue::stringValue)
            .orElseGet(propertyDescriptor::getName);
    }

    private static <T> boolean isMappableProperty(Class<T> immutableClass, BeanProperty<T, ?> propertyDescriptor) {

        if (propertyDescriptor.isWriteOnly()) {
            debugLog(immutableClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is write only.");
            return false;
        }

        if (propertyDescriptor.isAnnotationPresent(DynamoDbIgnore.class)) {
            debugLog(immutableClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is ignored.");
            return false;
        }

        if (propertyDescriptor.isAnnotationPresent(Ignore.class)) {
            debugLog(immutableClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is ignored.");
            return false;
        }

        return true;
    }

    private static void debugLog(Class<?> immutableClass, Supplier<String> logMessage) {
        BEAN_LOGGER.debug(() -> immutableClass.getTypeName() + " - " + logMessage.get());
    }

    private static Optional<AnnotationValue<Annotation>> findAnnotation(AnnotationMetadataProvider source, Class<? extends Annotation>... annotationClasses) {
        for (Class<? extends Annotation> annotationName : annotationClasses) {
            @SuppressWarnings("unchecked")
            Optional<AnnotationValue<Annotation>> maybeAnno = source.findAnnotation((Class<Annotation>) annotationName);
            if (maybeAnno.isPresent()) {
                return maybeAnno;
            }
        }
        return Optional.empty();
    }

    private static Optional<AnnotationValue<Annotation>> findAnnotation(AnnotationMetadataProvider source, Iterable<String> annotationNames) {
        for (String annotationName : annotationNames) {
            Optional<AnnotationValue<Annotation>> maybeAnno = source.findAnnotation(annotationName);
            if (maybeAnno.isPresent()) {
                return maybeAnno;
            }
        }
        return Optional.empty();
    }
}