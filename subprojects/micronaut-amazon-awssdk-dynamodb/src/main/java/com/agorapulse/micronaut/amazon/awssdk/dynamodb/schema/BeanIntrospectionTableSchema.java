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
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.WrappedTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of a bean
 * class. Example:
 * <pre>
 * <code>
 * {@literal @}Introspected
 * public class Customer {
 *     private String accountId;
 *     private int subId;            // primitive types are supported
 *     private String name;
 *     private Instant createdDate;
 *
 *     {@literal @}PartitionKey
 *     public String getAccountId() { return this.accountId; }
 *     public void setAccountId(String accountId) { this.accountId = accountId; }
 *
 *     {@literal @}SortKey
 *     public int getSubId() { return this.subId; }
 *     public void setSubId(int subId) { this.subId = subId; }
 *
 *     // Defines a GSI (customers_by_name) with a partition key of 'name'
 *     {@literal @}SecondaryPartitionKey(indexNames = "customers_by_name")
 *     public String getName() { return this.name; }
 *     public void setName(String name) { this.name = name; }
 *
 *     // Defines an LSI (customers_by_date) with a sort key of 'createdDate' and also declares the
 *     // same attribute as a sort key for the GSI named 'customers_by_name'
 *     {@literal @}SecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})
 *     public Instant getCreatedDate() { return this.createdDate; }
 *     public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }
 * }
 *
 * </pre>
 * <p>
 * Creating an {@link BeanIntrospectionTableSchema} is a moderately expensive operation, and should be performed sparingly. This is
 * usually done once at application startup.
 * <p>
 *
 * @param <T> The type of object that this {@link TableSchema} maps to.
 */
@SdkPublicApi
@ThreadSafe
public final class BeanIntrospectionTableSchema<T> extends WrappedTableSchema<T, StaticTableSchema<T>> {


    private BeanIntrospectionTableSchema(StaticTableSchema<T> staticTableSchema) {
        super(staticTableSchema);
    }

    public static <T> BeanIntrospectionTableSchema<T> create(Class<T> beanClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        IntrospectionTableSchema.debugLog(beanClass, () -> "Creating bean schema");
        // Fetch or create a new reference to this yet-to-be-created TableSchema in the cache
        MetaTableSchema<T> metaTableSchema = metaTableSchemaCache.getOrCreate(beanClass);

        BeanIntrospectionTableSchema<T> newTableSchema =
            new BeanIntrospectionTableSchema<>(createStaticTableSchema(beanClass, context, metaTableSchemaCache));
        if (!metaTableSchema.isInitialized()) {
            metaTableSchema.initialize(newTableSchema);
        }
        return newTableSchema;
    }

    // Called when creating an immutable TableSchema recursively. Utilizes the MetaTableSchema cache to stop infinite
    // recursion
    static <T> TableSchema<T> recursiveCreate(Class<T> beanClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        Optional<MetaTableSchema<T>> metaTableSchema = metaTableSchemaCache.get(beanClass);

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
        return create(beanClass, context, metaTableSchemaCache);

    }

    private static <T> StaticTableSchema<T> createStaticTableSchema(Class<T> beanClass,
                                                                    BeanContext beanContext,
                                                                    MetaTableSchemaCache metaTableSchemaCache) {
        Optional<BeanIntrospection<T>> introspectionOptional = BeanIntrospector.SHARED.findIntrospection(beanClass);


        if (introspectionOptional.isEmpty()) {
            throw new IllegalArgumentException("A DynamoDb bean class must be annotated with @Introspected, but " + beanClass.getTypeName() + " was not.");
        }

        BeanIntrospection<T> introspection = introspectionOptional.get();


        StaticTableSchema.Builder<T> builder = StaticTableSchema.builder(beanClass).newItemSupplier(introspection::instantiate);

        Optional<AnnotationValue<DynamoDbBean>> optionalDynamoDbBean = introspection.findAnnotation(DynamoDbBean.class);
        if (optionalDynamoDbBean.isPresent()) {
            builder.attributeConverterProviders(IntrospectionTableSchema.createConverterProvidersFromAnnotation(optionalDynamoDbBean.get(), beanContext));
        } else {
            builder.attributeConverterProviders(new LegacyAttributeConverterProvider());
        }

        List<StaticAttribute<T, ?>> attributes = new ArrayList<>();

        introspection.getBeanProperties().stream()
            .filter(p -> isMappableProperty(beanClass, p))
            .map(propertyDescriptor -> {
                propertyDescriptor.findAnnotation(TimeToLive.class).ifPresent(timeToLive ->
                    attributes.add(createTtlAttributeFromFieldAnnotation(beanClass, timeToLive, propertyDescriptor, beanContext))
                );
                return extractAttributeFromProperty(beanClass, metaTableSchemaCache, builder, propertyDescriptor, beanContext);
            })
            .filter(Objects::nonNull)
            .forEach(attributes::add);

        introspection.findAnnotation(TimeToLive.class).ifPresent(ttl -> attributes.add(createTtlAttributeFromTopLevelAnnotation(beanClass, ttl, beanContext)));

        builder.attributes(attributes);

        return builder.build();
    }

    private static <T> StaticAttribute<T, ?> createTtlAttributeFromTopLevelAnnotation(Class<T> type, AnnotationValue<TimeToLive> ttl, BeanContext beanContext) {
        long durationInSeconds = beanContext.getConversionService().convertRequired(ttl.getRequiredValue(String.class), Duration.class).getSeconds();
        return StaticAttribute.builder(type, Long.class)
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> System.currentTimeMillis() / 1000 + durationInSeconds)
            .setter((instance, value) -> { })
            .build();
    }

    private static <T> StaticAttribute<T, ?> createTtlAttributeFromFieldAnnotation(Class<T> type, AnnotationValue<TimeToLive> ttl, BeanProperty<T, ?> property, BeanContext beanContext) {
        Duration duration = beanContext.getConversionService().convertRequired(ttl.getRequiredValue(String.class), Duration.class);
        Function<T, Instant> toInstant = IntrospectionTableSchema.createInstantGetter(ttl, property, beanContext);

        return StaticAttribute.builder(type, Long.class)
            .name(ttl.stringValue("attributeName").filter(StringUtils::isNotEmpty).orElse("ttl"))
            .getter(instance -> Optional.ofNullable(toInstant.apply(instance)).orElseGet(Instant::now).plus(duration).getEpochSecond())
            .setter((instance, value) -> { })
            .build();
    }


    private static <T, P> StaticAttribute<T, P> extractAttributeFromProperty(
        Class<T> beanClass,
        MetaTableSchemaCache metaTableSchemaCache,
        StaticTableSchema.Builder<T> builder,
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext
    ) {
        Optional<AnnotationValue<Annotation>> dynamoDbFlatten = IntrospectionTableSchema.findAnnotation(propertyDescriptor, DynamoDbFlatten.class, Flatten.class);

        if (dynamoDbFlatten.isPresent()) {
            builder.flatten(
                IntrospectionTableSchema.create(propertyDescriptor.getType(), beanContext, metaTableSchemaCache),
                propertyDescriptor::get,
                propertyDescriptor::set
            );
            return null;
        } else {
            AttributeConfiguration attributeConfiguration = IntrospectionTableSchema.resolveAttributeConfiguration(propertyDescriptor);

            StaticAttribute.Builder<T, P> attributeBuilder = staticAttributeBuilder(propertyDescriptor, beanClass, metaTableSchemaCache, attributeConfiguration, beanContext);

            IntrospectionTableSchema.createAttributeConverterFromAnnotation(propertyDescriptor, beanContext).ifPresent(attributeBuilder::attributeConverter);

            addTagsToAttribute(attributeBuilder, propertyDescriptor);
            return attributeBuilder.build();
        }
    }



    private static <T, P> StaticAttribute.Builder<T, P> staticAttributeBuilder(
        BeanProperty<T, P> propertyDescriptor,
        Class<T> beanClass,
        MetaTableSchemaCache metaTableSchemaCache,
        AttributeConfiguration attributeConfiguration,
        BeanContext beanContext
    ) {
        Argument<P> propertyType = propertyDescriptor.asArgument();
        EnhancedType<P> propertyTypeToken = IntrospectionTableSchema.convertTypeToEnhancedType(propertyType, metaTableSchemaCache, attributeConfiguration, beanContext);
        return StaticAttribute.builder(beanClass, propertyTypeToken)
            .name(IntrospectionTableSchema.attributeNameForProperty(propertyDescriptor))
            .getter(propertyDescriptor::get)
            // secondary indices can be read only
            .setter(propertyDescriptor.isReadOnly() ? (bean, value) -> {} : propertyDescriptor::set);
    }



    /**
     * This method scans all the annotations on a property and looks for a meta-annotation of
     * {@link BeanTableSchemaAttributeTag}. If the meta-annotation is found, it attempts to create
     * an annotation tag based on a standard named static method
     * of the class that tag has been annotated with passing in the original property annotation as an argument.
     */
    private static <T> void addTagsToAttribute(StaticAttribute.Builder<?, ?> attributeBuilder,
                                               BeanProperty<T, ?> propertyDescriptor) {

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.UPDATE_BEHAVIOUR_ANNOTATIONS)
            .flatMap(anno -> anno.enumValue(Enum.class))
            .ifPresent(behavior -> attributeBuilder.addTag(StaticAttributeTags.updateBehavior(software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.valueOf(behavior.name()))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.PARTITION_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(StaticAttributeTags.primaryPartitionKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SORT_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(StaticAttributeTags.primarySortKey()));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_PARTITION_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(StaticAttributeTags.secondaryPartitionKey(Arrays.asList(indexNames))));

        IntrospectionTableSchema.findAnnotation(propertyDescriptor, IntrospectionTableSchema.SECONDARY_SORT_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(StaticAttributeTags.secondarySortKey(Arrays.asList(indexNames))));
    }



    private static <T> boolean isMappableProperty(Class<T> beanClass, BeanProperty<T, ?> propertyDescriptor) {

        if (propertyDescriptor.isWriteOnly()) {
            IntrospectionTableSchema.debugLog(beanClass, () -> "Ignoring bean property %s because it is write only.".formatted(propertyDescriptor.getName()));
            return false;
        }

        if (propertyDescriptor.isReadOnly()) {
            IntrospectionTableSchema.debugLog(beanClass, () -> "Ignoring bean property %s because it is read only.".formatted(propertyDescriptor.getName()));
            return isSecondaryIndex(propertyDescriptor);
        }

        return IntrospectionTableSchema.isNotIgnored(beanClass, propertyDescriptor);
    }

    private static <T> boolean isSecondaryIndex(BeanProperty<T, ?> propertyDescriptor) {
        return propertyDescriptor.getAnnotationNames().stream().anyMatch(name ->
            IntrospectionTableSchema.SECONDARY_PARTITION_KEYS_ANNOTATIONS.contains(name) || IntrospectionTableSchema.SECONDARY_SORT_KEYS_ANNOTATIONS.contains(name)
        );
    }


}

