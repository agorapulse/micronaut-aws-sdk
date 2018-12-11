package com.agorapulse.micronaut.aws.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.ReflectionUtils

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class DynamoDBMetadata<TItemClass> {

    private static ConcurrentHashMap<Class, DynamoDBMetadata> metadataCache = new ConcurrentHashMap<>()

    static <T> DynamoDBMetadata<T> create(Class<T> type) {
        return metadataCache.computeIfAbsent(type) {
            new DynamoDBMetadata<T>(it)
        }
    }

    final String hashKeyName
    final Class hashKeyClass
    final Class<TItemClass> itemClass
    final DynamoDBTable mainTable
    final String rangeKeyName
    final Class rangeKeyClass
    final List<String> secondaryIndexes

    private DynamoDBMetadata(Class<TItemClass> itemClass) {
        this.itemClass = itemClass
        this.mainTable = (DynamoDBTable) itemClass.getAnnotation(DynamoDBTable.class)

        if (!mainTable) {
            throw new RuntimeException("Missing @DynamoDBTable annotation on class: ${itemClass}")
        }

        List<String> secondaryIndexes = []

        // Annotations on fields
        for (Field field in itemClass.getDeclaredFields()) {
            // Get hash key
            if (field.getAnnotation(DynamoDBHashKey.class)) {
                hashKeyName = field.getName()
                hashKeyClass = field.getType()
            }
            // Get range key
            if (field.getAnnotation(DynamoDBRangeKey.class)) {
                rangeKeyName = field.getName()
                rangeKeyClass = field.getType()
            }
            // Get secondary indexes
            DynamoDBIndexRangeKey indexRangeKeyAnnotation = field.getAnnotation(DynamoDBIndexRangeKey.class)
            if (indexRangeKeyAnnotation) {
                secondaryIndexes.add(indexRangeKeyAnnotation.localSecondaryIndexName())
            }
        }

        // Annotations on methods
        for (Method method in itemClass.getDeclaredMethods()) {
            if (method.name.startsWith('get') || method.name.startsWith('is')) {
                // Get hash key
                if (method.getAnnotation(DynamoDBHashKey.class)) {
                    hashKeyName = ReflectionUtils.getFieldNameByGetter(method, true)
                    hashKeyClass = itemClass.getDeclaredField(hashKeyName).type
                }
                // Get range key
                if (method.getAnnotation(DynamoDBRangeKey.class)) {
                    rangeKeyName = ReflectionUtils.getFieldNameByGetter(method, true)
                    rangeKeyClass = itemClass.getDeclaredField(rangeKeyName).type
                }
                // Get secondary indexes
                DynamoDBIndexRangeKey indexRangeKeyAnnotation = method.getAnnotation(DynamoDBIndexRangeKey.class)
                if (indexRangeKeyAnnotation) {
                    secondaryIndexes.add(indexRangeKeyAnnotation.localSecondaryIndexName())
                }
            }
        }

        if (!hashKeyName || !hashKeyClass) {
            throw new RuntimeException("Missing hashkey annotations on class: ${itemClass}")
        }

        this.secondaryIndexes = secondaryIndexes.asImmutable()
    }
}
