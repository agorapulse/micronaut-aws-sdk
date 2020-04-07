package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import sun.font.AttributeValues;

import javax.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class BeanIntrospectionAttributeValueConverter implements AttributeValueConverter {

    @Override
    public <T> Map<String, AttributeValue> convert(DynamoDbTable<T> table, Map<String, Object> values) {
        BeanIntrospection<T> introspection = getBeanIntrospection(table);
        T instance = introspection.instantiate();
        return values.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> convert(introspection, table, instance, e.getKey(), e.getValue())
        ));
    }

    private <T> BeanIntrospection<T> getBeanIntrospection(DynamoDbTable<T> table) {
        return BeanIntrospector.SHARED.findIntrospection(table.tableSchema().itemType().rawClass())
            .orElseThrow(() -> new IllegalArgumentException("No introspection found for " + table.tableSchema().itemType().rawClass()
                + "! Please, see https://docs.micronaut.io/latest/guide/index.html#introspection for more details")
            );
    }

    private <T> AttributeValue convert(BeanIntrospection<T> introspection, DynamoDbTable<T> table, T instance, String key, Object value) {
        if (value instanceof AttributeValue) {
            return (AttributeValue) value;
        }
        introspection.getProperty(key).ifPresent(p -> p.set(instance, value));
        return table.tableSchema().attributeValue(instance, key);
    }

}
