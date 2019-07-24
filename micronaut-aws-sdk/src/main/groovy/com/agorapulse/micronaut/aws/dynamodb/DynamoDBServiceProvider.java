package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider of {@link DynamoDBService} for particular DynamoDB entities.
 */
@Singleton
@Requires(classes = IDynamoDBMapper.class)
public class DynamoDBServiceProvider {

    private final ConcurrentHashMap<Class, DynamoDBService> serviceCache = new ConcurrentHashMap<>();
    private final AmazonDynamoDB client;
    private final IDynamoDBMapper mapper;

    public DynamoDBServiceProvider(AmazonDynamoDB client, IDynamoDBMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Provides {@link DynamoDBService} for given type.
     *
     * @param type DynamoDB entity type.
     * @param <T> the type of the DynamoDB entity
     * @return {@link DynamoDBService} for given type
     */
    public <T> DynamoDBService<T> findOrCreate(Class<T> type) {
        return serviceCache.computeIfAbsent(type, (t) -> new DefaultDynamoDBService<T>(client, mapper, type));
    }

}
