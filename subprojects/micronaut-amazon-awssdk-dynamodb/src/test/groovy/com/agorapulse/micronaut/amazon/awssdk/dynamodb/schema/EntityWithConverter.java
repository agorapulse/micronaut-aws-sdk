package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.Introspected;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Introspected
@DynamoDbBean(converterProviders = EntityWithConverter.SubEntityConverterProvider.class)
public class EntityWithConverter {

    @Introspected
    public static class SubEntityConverterProvider implements AttributeConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> AttributeConverter<T> converterFor(EnhancedType<T> type) {
            if (type.equals(EnhancedType.of(SubEntity.class))) {
                return (AttributeConverter<T>) new SubEntityConverter();
            }
            return AttributeConverterProvider.defaultProvider().converterFor(type);
        }

    }

    @Introspected
    public static class SubEntityConverter implements AttributeConverter<SubEntity> {

        @Override
        public AttributeValue transformFrom(SubEntity input) {
            if (input == null) {
                return AttributeValue.fromNul(true);
            }
            return AttributeValue.fromS(input.getName());
        }

        @Override
        public SubEntity transformTo(AttributeValue input) {
            if (input.nul() != null && input.nul()) {
                return null;
            }

            SubEntity subEntity = new SubEntity();
            subEntity.setName(input.s());
            return subEntity;
        }

        @Override
        public EnhancedType<SubEntity> type() {
            return EnhancedType.of(SubEntity.class);
        }

        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.S;
        }

    }

    @Introspected
    public static class SubEntity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @PartitionKey
    private String id;
    private SubEntity subEntity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubEntity getSubEntity() {
        return subEntity;
    }

    public void setSubEntity(SubEntity subEntity) {
        this.subEntity = subEntity;
    }

}
