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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Flatten;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.TimeToLive;
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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of an immutable
 * class using Micronaut bean introspection instead of reflection. This is based on AWS SDK's ImmutableTableSchema
 * but adapted to work with Micronaut's introspection capabilities.
 * <p>
 * Supports immutable classes with {@link DynamoDbImmutable} annotation (via annotation processor)
 * or classes annotated with {@literal @}Introspected with builder configuration.
 * <p>
 * Example with @DynamoDbImmutable annotation:
 * <pre>
 * <code>
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
 *
 * Example with @Introspected builder annotation:
 * <pre>
 * <code>
 * {@literal @}Introspected(builder = {@literal @}Introspected.IntrospectionBuilder(builderClass = Customer.Builder.class))
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
 */
@SdkPublicApi
@ThreadSafe
public final class ImmutableBeanIntrospectionTableSchema<T> extends WrappedTableSchema<T, StaticImmutableTableSchema<T, BeanIntrospection.Builder<T>>> {

    private ImmutableBeanIntrospectionTableSchema(StaticImmutableTableSchema<T, BeanIntrospection.Builder<T>> staticImmutableTableSchema) {
        super(staticImmutableTableSchema);
    }

    public static <T> ImmutableBeanIntrospectionTableSchema<T> create(Class<T> immutableClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        if (!IntrospectionTableSchema.isImmutableClass(immutableClass)) {
            throw new IllegalArgumentException("The class " + immutableClass.getName() + " is not immutable. Use BeanIntrospectionTableSchema instead.");
        }
        IntrospectionTableSchema.debugLog(immutableClass, () -> "Creating immutable bean introspection schema");
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(immutableClass);

        ImmutableBeanIntrospectionTableSchema<T> newTableSchema = new ImmutableBeanIntrospectionTableSchema<>(createStaticImmutableTableSchema(immutableClass, context, metaTableSchemaCache));
        if (!metaTableSchema.isInitialized()) {
            metaTableSchema.initialize(newTableSchema);
        }
        return newTableSchema;
    }


    private static <T> StaticImmutableTableSchema<T, BeanIntrospection.Builder<T>> createStaticImmutableTableSchema(
        Class<T> immutableClass,
        BeanContext beanContext,
        MetaTableSchemaCache metaTableSchemaCache) {

        Optional<BeanIntrospection<T>> immutableIntrospectionOptional = BeanIntrospector.SHARED.findIntrospection(immutableClass);

        if (immutableIntrospectionOptional.isEmpty()) {
            throw new IllegalArgumentException("An immutable DynamoDb class must be annotated with @Introspected, but " + immutableClass.getTypeName() + " was not.");
        }

        BeanIntrospection<T> immutableIntrospection = immutableIntrospectionOptional.get();

        if (!immutableIntrospection.hasBuilder()) {
            throw new IllegalArgumentException("An immutable DynamoDb class must have a builder configured in @Introspected, but " + immutableClass.getTypeName() + " does not.");
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<BeanIntrospection.Builder<T>> builderClass = (Class) BeanIntrospection.Builder.class;

        StaticImmutableTableSchema.Builder<T, BeanIntrospection.Builder<T>> builder = StaticImmutableTableSchema
            .builder(immutableClass, builderClass)
            .newItemBuilder(immutableIntrospection::builder, BeanIntrospection.Builder::build);

        Optional<AnnotationValue<DynamoDbBean>> optionalDynamoDbBean = immutableIntrospection.findAnnotation(DynamoDbBean.class);
        Optional<AnnotationValue<DynamoDbImmutable>> optionalDynamoDbImmutable = immutableIntrospection.findAnnotation(DynamoDbImmutable.class);

        List<AttributeConverterProvider> attributeConverterProviders = optionalDynamoDbBean
            .map(dynamoDbBeanAnnotationValue -> IntrospectionTableSchema.createConverterProvidersFromAnnotation(dynamoDbBeanAnnotationValue, beanContext))
            .orElseGet(() -> optionalDynamoDbImmutable
                .map(dynamoDbImmutableAnnotationValue -> IntrospectionTableSchema.createConverterProvidersFromAnnotation(dynamoDbImmutableAnnotationValue, beanContext))
                .orElseGet(() -> List.of(new LegacyAttributeConverterProvider()))
            );

        builder.attributeConverterProviders(attributeConverterProviders);

        List<ImmutableAttribute<T, BeanIntrospection.Builder<T>, ?>> attributes = new ArrayList<>();

        immutableIntrospection.getBeanProperties().stream()
            .filter(p -> isMappableProperty(immutableClass, p))
            .map(propertyDescriptor -> {
                propertyDescriptor.findAnnotation(TimeToLive.class).ifPresent(timeToLive ->
                    attributes.add(createTtlAttributeFromFieldAnnotation(immutableClass, builderClass, timeToLive, propertyDescriptor, beanContext))
                );
                return extractAttributeFromProperty(immutableClass, builderClass, metaTableSchemaCache, builder, propertyDescriptor, beanContext, attributeConverterProviders);
            })
            .filter(Objects::nonNull)
            .forEach(attributes::add);

        immutableIntrospection.findAnnotation(TimeToLive.class).ifPresent(ttl ->
            attributes.add(createTtlAttributeFromTopLevelAnnotation(immutableClass, builderClass, ttl, beanContext)));

        builder.attributes(attributes);

        return builder.build();
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

    private static <T> ImmutableAttribute<T, BeanIntrospection.Builder<T>, ?> createTtlAttributeFromFieldAnnotation(
        Class<T> immutableClass, Class<BeanIntrospection.Builder<T>> builderClass, AnnotationValue<TimeToLive> ttl, BeanProperty<T, ?> property, BeanContext beanContext) {

        Duration duration = beanContext.getConversionService().convertRequired(ttl.getRequiredValue(String.class), Duration.class);
        Function<T, Instant> toInstant = IntrospectionTableSchema.createInstantGetter(ttl, property, beanContext);

        return ImmutableAttribute.builder(immutableClass, builderClass, EnhancedType.of(Long.class))
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> Optional.ofNullable(toInstant.apply(instance)).orElseGet(Instant::now).plus(duration).getEpochSecond())
            .setter((builder, value) -> { }) // TTL is read-only
            .build();
    }


    private static <T, P> ImmutableAttribute<T, BeanIntrospection.Builder<T>, P> extractAttributeFromProperty(
        Class<T> immutableClass,
        Class<BeanIntrospection.Builder<T>> builderClass,
        MetaTableSchemaCache metaTableSchemaCache,
        StaticImmutableTableSchema.Builder<T, BeanIntrospection.Builder<T>> builder,
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext,
        List<AttributeConverterProvider> attributeConverterProviders
    ) {
        Optional<AnnotationValue<Annotation>> dynamoDbFlatten = IntrospectionTableSchema.findAnnotation(propertyDescriptor, DynamoDbFlatten.class, Flatten.class);

        if (dynamoDbFlatten.isPresent()) {
            // Find corresponding setter in builder
            builder.flatten(
                IntrospectionTableSchema.create(propertyDescriptor.getType(), beanContext, metaTableSchemaCache),
                propertyDescriptor::get,
                (b, value) -> b.with(propertyDescriptor.getName(), value)
            );
            return null;
        } else {
            AttributeConfiguration attributeConfiguration = IntrospectionTableSchema.resolveAttributeConfiguration(propertyDescriptor);

            ImmutableAttribute.Builder<T, BeanIntrospection.Builder<T>, P> attributeBuilder = immutableAttributeBuilder(propertyDescriptor, immutableClass, builderClass, metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);

            IntrospectionTableSchema.createAttributeConverterFromAnnotation(propertyDescriptor, beanContext).ifPresent(attributeBuilder::attributeConverter);

            IntrospectionTableSchema.collectTagsToAttribute(propertyDescriptor).forEach(attributeBuilder::addTag);

            return attributeBuilder.build();
        }
    }

    private static <T, P> ImmutableAttribute.Builder<T, BeanIntrospection.Builder<T>, P> immutableAttributeBuilder(
        BeanProperty<T, P> propertyDescriptor,
        Class<T> immutableClass,
        Class<BeanIntrospection.Builder<T>> builderClass,
        MetaTableSchemaCache metaTableSchemaCache,
        AttributeConfiguration attributeConfiguration,
        BeanContext beanContext,
        List<AttributeConverterProvider> attributeConverterProviders
    ) {
        Argument<P> propertyType = propertyDescriptor.asArgument();
        EnhancedType<P> propertyTypeToken = IntrospectionTableSchema.convertTypeToEnhancedType(propertyType, metaTableSchemaCache, attributeConfiguration, beanContext, attributeConverterProviders);

        return ImmutableAttribute.builder(immutableClass, builderClass, propertyTypeToken)
            .name(IntrospectionTableSchema.attributeNameForProperty(propertyDescriptor))
            .getter(propertyDescriptor::get)
            .setter((builder, value) -> builder.with(propertyDescriptor.getName(), value));
    }

    private static <T> boolean isMappableProperty(Class<T> immutableClass, BeanProperty<T, ?> propertyDescriptor) {
        if (propertyDescriptor.isWriteOnly()) {
            IntrospectionTableSchema.debugLog(immutableClass, () -> "Ignoring bean property %s because it is write only.".formatted(propertyDescriptor.getName()));
            return false;
        }

        return IntrospectionTableSchema.isNotIgnored(immutableClass, propertyDescriptor);
    }

}
