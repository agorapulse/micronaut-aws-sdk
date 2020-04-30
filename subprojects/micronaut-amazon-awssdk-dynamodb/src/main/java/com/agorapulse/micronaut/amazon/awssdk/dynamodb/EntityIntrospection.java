package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

class EntityIntrospection {

    static <T> BeanIntrospection<T> getBeanIntrospection(DynamoDbTable<T> table) {
        return BeanIntrospector.SHARED.findIntrospection(table.tableSchema().itemType().rawClass())
            .orElseThrow(() -> new IllegalArgumentException("No introspection found for " + table.tableSchema().itemType().rawClass()
                + "! Please, annotate the class with @Introspected. See https://docs.micronaut.io/latest/guide/index.html#introspection for more details")
            );
    }

}
