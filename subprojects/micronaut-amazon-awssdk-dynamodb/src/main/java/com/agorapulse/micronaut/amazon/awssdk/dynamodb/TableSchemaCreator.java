package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public interface TableSchemaCreator {

    /**
     * Creates new table schema for given entity.
     * @param entity entity for which the table schema should be generated
     * @param <T> type of the entity
     * @return new table schema for given entity
     */
    <T> TableSchema<T> create(Class<T> entity);

}
