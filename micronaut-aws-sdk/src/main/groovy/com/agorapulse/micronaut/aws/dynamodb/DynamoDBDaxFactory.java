package com.agorapulse.micronaut.aws.dynamodb;

import com.amazon.dax.client.dynamodbv2.AmazonDaxClient;
import com.amazon.dax.client.dynamodbv2.AmazonDaxClientBuilder;
import com.amazon.dax.client.dynamodbv2.ClientConfig;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Factory class which provides the {@link AmazonDynamoDB} instance backed by {@link AmazonDaxClient}.
 */
@Factory
@Requires(classes = AmazonDaxClient.class)
public class DynamoDBDaxFactory {

    @Bean
    @Singleton
    @Requires(property = "aws.dax.endpoint")
    AmazonDynamoDB amazonDynamoDB(
        @Value("${aws.dax.endpoint}") String endpoint,
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider
    ) {
        ClientConfig clientConfig = migrateClientConfig(clientConfiguration.getClientConfiguration())
            .withEndpoints(endpoint)
            .withRegion(awsRegionProvider.getRegion())
            .withCredentialsProvider(credentialsProvider);
        return AmazonDaxClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsRegionProvider.getRegion())
            .withClientConfiguration(clientConfig)
            .build();
    }

    private ClientConfig migrateClientConfig(ClientConfiguration clientConfiguration) {
        return new ClientConfig()
            .withConnectTimeout(clientConfiguration.getConnectionTimeout(), TimeUnit.MILLISECONDS)
            .withMaxPendingConnectsPerHost(clientConfiguration.getMaxConnections())
            .withReadRetries(clientConfiguration.getMaxErrorRetry())
            .withWriteRetries(clientConfiguration.getMaxErrorRetry());
    }
}
