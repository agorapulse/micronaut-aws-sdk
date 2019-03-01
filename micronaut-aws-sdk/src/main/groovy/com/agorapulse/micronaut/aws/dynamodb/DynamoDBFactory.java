package com.agorapulse.micronaut.aws.dynamodb;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Factory class which provides the {@link AmazonDynamoDB} and {@link IDynamoDBMapper} into the application context.
 */
@Factory
@Requires(classes = DynamoDB.class)
public class DynamoDBFactory {

    @Bean
    @Singleton
    @Requires(missingProperty = "aws.dax.endpoint")
    AmazonDynamoDB amazonDynamoDB(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.dynamodb.region}") Optional<String> region
    ) {
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Bean
    @Singleton
    IDynamoDBMapper dynamoDBMapper(AmazonDynamoDB client) {
        return new DynamoDBMapper(client);
    }

}
