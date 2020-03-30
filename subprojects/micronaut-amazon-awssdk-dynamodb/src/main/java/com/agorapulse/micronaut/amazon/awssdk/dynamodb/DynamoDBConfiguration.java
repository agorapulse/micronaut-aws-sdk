package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;

import javax.inject.Named;

/**
 * Default DynamoDB configuration.
 */
@Named("default")
@ConfigurationProperties("aws.dynamodb")
@Requires(classes = software.amazon.awssdk.services.dynamodb.DynamoDbClient.class)
public class DynamoDBConfiguration extends DefaultRegionAndEndpointConfiguration {

}
