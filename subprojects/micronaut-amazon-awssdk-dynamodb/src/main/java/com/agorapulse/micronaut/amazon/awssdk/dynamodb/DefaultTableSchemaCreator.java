package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;

import javax.inject.Singleton;

@Singleton
public class DefaultTableSchemaCreator implements TableSchemaCreator {

    @Override
    public <T> TableSchema<T> create(Class<T> entity) {
        return BeanTableSchema.create(entity);
    }
    
}
