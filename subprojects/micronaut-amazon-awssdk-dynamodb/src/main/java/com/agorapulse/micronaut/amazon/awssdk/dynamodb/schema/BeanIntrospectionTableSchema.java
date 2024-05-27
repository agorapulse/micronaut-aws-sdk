/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert.LegacyAttributeConverterProvider;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationMetadataProvider;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
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
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.WrappedTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.BeanTableSchemaAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbFlatten;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPreserveEmptyObject;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static software.amazon.awssdk.enhanced.dynamodb.internal.DynamoDbEnhancedLogger.BEAN_LOGGER;

/**
 * Implementation of {@link TableSchema} that builds a table schema based on properties and annotations of a bean
 * class. Example:
 * <pre>
 * <code>
 * {@literal @}DynamoDbBean
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

    private BeanIntrospectionTableSchema(StaticTableSchema<T> staticTableSchema) {
        super(staticTableSchema);
    }

    public static <T> BeanIntrospectionTableSchema<T> create(Class<T> beanClass, BeanContext context, MetaTableSchemaCache metaTableSchemaCache) {
        debugLog(beanClass, () -> "Creating bean schema");
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

            // Or: return the uninitialized MetaTableSchema as this must be a recursive reference and it will be
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


        if (!introspectionOptional.isPresent()) {
            throw new IllegalArgumentException("A DynamoDb bean class must be annotated with @Introspected, but " + beanClass.getTypeName() + " was not.");
        }

        BeanIntrospection<T> introspection = introspectionOptional.get();


        StaticTableSchema.Builder<T> builder = StaticTableSchema.builder(beanClass).newItemSupplier(introspection::instantiate);

        Optional<AnnotationValue<DynamoDbBean>> optionalDynamoDbBean = introspection.findAnnotation(DynamoDbBean.class);
        if (optionalDynamoDbBean.isPresent()) {
            builder.attributeConverterProviders(createConverterProvidersFromAnnotation(beanClass, optionalDynamoDbBean.get(), beanContext));
        } else {
            builder.attributeConverterProviders(new LegacyAttributeConverterProvider());
        }

        List<StaticAttribute<T, ?>> attributes = introspection.getBeanProperties().stream()
            .filter(p -> isMappableProperty(beanClass, p))
            .map(propertyDescriptor -> extractAttributeFromProperty(beanClass, metaTableSchemaCache, builder, propertyDescriptor, beanContext))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        builder.attributes(attributes);

        return builder.build();
    }

    private static <T, P> StaticAttribute<T, P> extractAttributeFromProperty(
        Class<T> beanClass,
        MetaTableSchemaCache metaTableSchemaCache,
        StaticTableSchema.Builder<T> builder,
        BeanProperty<T, P> propertyDescriptor,
        BeanContext beanContext
    ) {
        Optional<AnnotationValue<Annotation>> dynamoDbFlatten = findAnnotation(propertyDescriptor, DynamoDbFlatten.class, Flatten.class);

        if (dynamoDbFlatten.isPresent()) {
            builder.flatten(
                BeanIntrospectionTableSchema.create(propertyDescriptor.getType(), beanContext, metaTableSchemaCache),
                propertyDescriptor::get,
                propertyDescriptor::set
            );
            return null;
        } else {
            AttributeConfiguration attributeConfiguration = resolveAttributeConfiguration(propertyDescriptor);

            StaticAttribute.Builder<T, P> attributeBuilder = staticAttributeBuilder(propertyDescriptor, beanClass, metaTableSchemaCache, attributeConfiguration, beanContext);

            createAttributeConverterFromAnnotation(propertyDescriptor, beanContext).ifPresent(attributeBuilder::attributeConverter);

            addTagsToAttribute(attributeBuilder, propertyDescriptor);
            return attributeBuilder.build();
        }
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
    private static List<AttributeConverterProvider> createConverterProvidersFromAnnotation(Class<?> beanClass, AnnotationValue<DynamoDbBean> dynamoDbBean, BeanContext beanContext) {
        Class<? extends AttributeConverterProvider>[] providerClasses = (Class<? extends AttributeConverterProvider>[]) dynamoDbBean.classValues("converterProviders");
        if (providerClasses.length == 0) {
            providerClasses = new Class[]{LegacyAttributeConverterProvider.class};
        }

        return Arrays.stream(providerClasses)
            .peek(c -> debugLog(beanClass, () -> "Adding Converter: " + c.getTypeName()))
            .map(c -> (AttributeConverterProvider) fromContextOrNew(c, beanContext).get())
            .collect(Collectors.toList());
    }

    private static <T, P> StaticAttribute.Builder<T, P> staticAttributeBuilder(
        BeanProperty<T, P> propertyDescriptor,
        Class<T> beanClass,
        MetaTableSchemaCache metaTableSchemaCache,
        AttributeConfiguration attributeConfiguration,
        BeanContext beanContext
    ) {
        Argument<P> propertyType = propertyDescriptor.asArgument();
        EnhancedType<P> propertyTypeToken = convertTypeToEnhancedType(propertyType, metaTableSchemaCache, attributeConfiguration, beanContext);
        return StaticAttribute.builder(beanClass, propertyTypeToken)
            .name(attributeNameForProperty(propertyDescriptor))
            .getter(propertyDescriptor::get)
            // secondary indices can be read only
            .setter(propertyDescriptor.isReadOnly() ? (bean, value) -> {} : propertyDescriptor::set);
    }

    /**
     * Converts a {@link Type} to an {@link EnhancedType}. Usually {@link EnhancedType#of} is capable of doing this all
     * by itself, but for the BeanTableSchema we want to detect if a parameterized class is being passed without a
     * converter that is actually another annotated class in which case we want to capture its schema and add it to the
     * EnhancedType. Unfortunately this means we have to duplicate some of the recursive Type parsing that
     * EnhancedClient otherwise does all by itself.
     */
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
            if (introspection.isPresent() && introspection.get().isAnnotationPresent(DynamoDbBean.class)) {
                Consumer<EnhancedTypeDocumentConfiguration.Builder> attrConfiguration = b -> b
                    .preserveEmptyObject(attributeConfiguration.preserveEmptyObject())
                    .ignoreNulls(attributeConfiguration.ignoreNulls());

                return EnhancedType.documentOf(
                    clazz,
                    BeanIntrospectionTableSchema.recursiveCreate(clazz, beanContext, metaTableSchemaCache),
                    attrConfiguration
                );
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
            .map(clazz -> (AttributeConverter<P>) fromContextOrNew(clazz, beanContext).get());
    }

    /**
     * This method scans all the annotations on a property and looks for a meta-annotation of
     * {@link BeanTableSchemaAttributeTag}. If the meta-annotation is found, it attempts to create
     * an annotation tag based on a standard named static method
     * of the class that tag has been annotated with passing in the original property annotation as an argument.
     */
    private static <T> void addTagsToAttribute(StaticAttribute.Builder<?, ?> attributeBuilder,
                                               BeanProperty<T, ?> propertyDescriptor) {

        findAnnotation(propertyDescriptor, UPDATE_BEHAVIOUR_ANNOTATIONS)
            .flatMap(anno -> anno.enumValue(Enum.class))
            .ifPresent(behavior -> attributeBuilder.addTag(StaticAttributeTags.updateBehavior(software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior.valueOf(behavior.name()))));

        findAnnotation(propertyDescriptor, PARTITION_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(StaticAttributeTags.primaryPartitionKey()));

        findAnnotation(propertyDescriptor, SORT_KEYS_ANNOTATIONS)
            .ifPresent(anno -> attributeBuilder.addTag(StaticAttributeTags.primarySortKey()));

        findAnnotation(propertyDescriptor, SECONDARY_PARTITION_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(StaticAttributeTags.secondaryPartitionKey(Arrays.asList(indexNames))));

        findAnnotation(propertyDescriptor, SECONDARY_SORT_KEYS_ANNOTATIONS)
            .map(anno -> anno.stringValues("indexNames"))
            .ifPresent(indexNames -> attributeBuilder.addTag(StaticAttributeTags.secondarySortKey(Arrays.asList(indexNames))));
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
            return ObjectConstructor.create(clazz, constructor);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("Class '%s' appears to have no default constructor thus cannot be used with the BeanTableSchema", clazz), e);
        }
    }

    private static <T> String attributeNameForProperty(BeanProperty<T, ?> propertyDescriptor) {
        return findAnnotation(propertyDescriptor, DynamoDbAttribute.class, Attribute.class)
            .flatMap(AnnotationValue::stringValue)
            .orElseGet(propertyDescriptor::getName);
    }

    private static <T> boolean isMappableProperty(Class<T> beanClass, BeanProperty<T, ?> propertyDescriptor) {

        if (propertyDescriptor.isWriteOnly()) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is write only.");
            return false;
        }

        if (propertyDescriptor.isReadOnly()) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is read only.");
            return isSecondaryIndex(propertyDescriptor);
        }

        if (propertyDescriptor.isAnnotationPresent(DynamoDbIgnore.class)) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is ignored.");
            return false;
        }

        if (propertyDescriptor.isAnnotationPresent(Ignore.class)) {
            debugLog(beanClass, () -> "Ignoring bean property " + propertyDescriptor.getName() + " because it is ignored.");
            return false;
        }

        return true;
    }

    private static <T> boolean isSecondaryIndex(BeanProperty<T, ?> propertyDescriptor) {
        return propertyDescriptor.getAnnotationNames().stream().anyMatch(name ->
            SECONDARY_PARTITION_KEYS_ANNOTATIONS.contains(name) || SECONDARY_SORT_KEYS_ANNOTATIONS.contains(name)
        );
    }

    private static void debugLog(Class<?> beanClass, Supplier<String> logMessage) {
        BEAN_LOGGER.debug(() -> beanClass.getTypeName() + " - " + logMessage.get());
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

