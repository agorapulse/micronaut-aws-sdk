package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Provider of {@link DynamoDBService} for particular DynamoDB entities.
 */
@Singleton
@Requires(classes = IDynamoDBMapper.class)
public class DynamoDBServiceProvider {

    private final AmazonDynamoDB client;
    private final IDynamoDBMapper mapper;
    private final BeanContext context;

    public DynamoDBServiceProvider(AmazonDynamoDB client, IDynamoDBMapper mapper, BeanContext context) {
        this.client = client;
        this.mapper = mapper;
        this.context = context;
    }

    /**
     * Provides {@link DynamoDBService} for given type.
     *
     * @param type DynamoDB entity type.
     * @param <T> the type of the DynamoDB entity
     * @return {@link DynamoDBService} for given type
     */
    public <T> DynamoDBService<T> findOrCreate(Class<T> type) {
        Optional<DynamoDBService> existingService = context.findBean(DynamoDBService.class, Qualifiers.byTypeArguments(type));

        if (existingService.isPresent()) {
            return (DynamoDBService<T>) existingService.get();
        }

        DynamoDBService<T> service = new DefaultDynamoDBService<>(client, mapper, type);

        context.registerSingleton(DynamoDBService.class, service, Qualifiers.byTypeArguments(type));

        return service;
    }

}
