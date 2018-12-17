package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.context.BeanContext;
import io.micronaut.inject.qualifiers.Qualifiers;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class DynamoDBServiceFactory {

    private final AmazonDynamoDB client;
    private final IDynamoDBMapper mapper;
    private final BeanContext context;

    public DynamoDBServiceFactory(AmazonDynamoDB client, IDynamoDBMapper mapper, BeanContext context) {
        this.client = client;
        this.mapper = mapper;
        this.context = context;
    }

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
