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
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.LegacyAttributeConverterProvider;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.WrappedTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of an immutable
 * class using Micronaut bean introspection instead of reflection. This is based on AWS SDK's ImmutableTableSchema
 * but adapted to work with Micronaut's introspection capabilities.
 * <p>
 * Supports traditional immutable classes with {@link DynamoDbImmutable} annotation, classes with the more
 * concise {@link Immutable} annotation, and Java records with a static builder() method.
 * <p>
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

    private ImmutableBeanIntrospectionTableSchema(StaticImmutableTableSchema<T, B> staticImmutableTableSchema) {
        super(staticImmutableTableSchema);
    }

    public static <T, B> ImmutableBeanIntrospectionTableSchema<T, B> create(Class<T> immutableClass, Class<B> builderClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        IntrospectionTableSchema.debugLog(immutableClass, () -> "Creating immutable bean introspection schema");
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

            // Or: return the uninitialized MetaTableSchema as this must be a recursive reference, and it will be
            // initialized later as the chain completes
            return metaTableSchema.get();
        }

        // Otherwise: cache doesn't know about this class; create a new one from scratch
        @SuppressWarnings("unchecked")
        Class<Object> builderClass = (Class<Object>) IntrospectionTableSchema.determineBuilderClass(immutableClass);

        return create(immutableClass, builderClass, context, metaTableSchemaCache);
    }


    private static <T, B> StaticImmutableTableSchema<T, B> createStaticImmutableTableSchema(
        Class<T> immutableClass,
        Class<B> builderClass,
        BeanContext beanContext,
        MetaTableSchemaCache metaTableSchemaCache) {

        Optional<BeanIntrospection<T>> immutableIntrospectionOptional = BeanIntrospector.SHARED.findIntrospection(immutableClass);
        Optional<BeanIntrospection<B>> builderIntrospectionOptional = BeanIntrospector.SHARED.findIntrospection(builderClass);

        if (immutableIntrospectionOptional.isEmpty()) {
            throw new IllegalArgumentException("An immutable DynamoDb class must be annotated with @Introspected, but " + immutableClass.getTypeName() + " was not.");
        }

        if (builderIntrospectionOptional.isEmpty()) {
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
            builder.attributeConverterProviders(IntrospectionTableSchema.createConverterProvidersFromAnnotation(optionalDynamoDbBean.get(), beanContext));
        } else if (optionalDynamoDbImmutable.isPresent()) {
            builder.attributeConverterProviders(IntrospectionTableSchema.createConverterProvidersFromAnnotation(optionalDynamoDbImmutable.get(), beanContext));
        } else if (optionalImmutable.isPresent()) {
            builder.attributeConverterProviders(IntrospectionTableSchema.createConverterProvidersFromAnnotation(optionalImmutable.get(), beanContext));
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

    @SuppressWarnings("unchecked")
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
        Function<T, Instant> toInstant = IntrospectionTableSchema.createInstantGetter(ttl, property, beanContext);

        return ImmutableAttribute.builder(immutableClass, builderClass, EnhancedType.of(Long.class))
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> Optional.ofNullable(toInstant.apply(instance)).orElseGet(Instant::now).plus(duration).getEpochSecond())
            .setter((builder, value) -> { }) // TTL is read-only
            .build();
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
        Optional<AnnotationValue<Annotation>> dynamoDbFlatten = IntrospectionTableSchema.findAnnotation(propertyDescriptor, DynamoDbFlatten.class, Flatten.class);

        if (dynamoDbFlatten.isPresent()) {
            // Find corresponding setter in builder
            BeanProperty<B, P> builderProperty = findBuilderProperty(builderIntrospection, propertyDescriptor.getName());

            builder.flatten(
                IntrospectionTableSchema.create(propertyDescriptor.getType(), beanContext, metaTableSchemaCache),
                propertyDescriptor::get,
                builderProperty != null ? builderProperty::set : (builderInstance, value) -> { }
            );
            return null;
        } else {
            AttributeConfiguration attributeConfiguration = IntrospectionTableSchema.resolveAttributeConfiguration(propertyDescriptor);

            ImmutableAttribute.Builder<T, B, P> attributeBuilder = immutableAttributeBuilder(
                propertyDescriptor, builderIntrospection, immutableClass, builderClass, metaTableSchemaCache, attributeConfiguration, beanContext);

            IntrospectionTableSchema.createAttributeConverterFromAnnotation(propertyDescriptor, beanContext).ifPresent(attributeBuilder::attributeConverter);

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
        EnhancedType<P> propertyTypeToken = IntrospectionTableSchema.convertTypeToEnhancedType(propertyType, metaTableSchemaCache, attributeConfiguration, beanContext);

        // Find corresponding setter in builder
        BeanProperty<B, P> builderProperty = findBuilderProperty(builderIntrospection, propertyDescriptor.getName());

        return ImmutableAttribute.builder(immutableClass, builderClass, propertyTypeToken)
            .name(IntrospectionTableSchema.attributeNameForProperty(propertyDescriptor))
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

    private static <T> void addTagsToAttribute(ImmutableAttribute.Builder<?, ?, ?> attributeBuilder,
                                               BeanProperty<T, ?> propertyDescriptor) {

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.UPDATE_BEHAVIOUR_ANNOTATIONS)
            .flatMap(anno -> anno.enumValue(Enum.class))
            .ifPresent(behavior -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.updateBehavior(software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.valueOf(behavior.name()))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.PARTITION_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SORT_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_PARTITION_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey(Arrays.asList(indexNames))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_SORT_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey(Arrays.asList(indexNames))));
    }



    private static <T> boolean isMappableProperty(Class<T> immutableClass, BeanProperty<T, ?> propertyDescriptor) {

        if (propertyDescriptor.isWriteOnly()) {
            IntrospectionTableSchema.debugLog(immutableClass, () -> "Ignoring bean property %s because it is write only.".formatted(propertyDescriptor.getName()));
            return false;
        }

        return IntrospectionTableSchema.isNotIgnored(immutableClass, propertyDescriptor);
    }

}
