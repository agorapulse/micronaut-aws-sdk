package com.agorapulse.micronaut.aws.cloudwatch;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

/**
 * Factory for providing CloudWatch.
 *
 * This is very basic support for CloudWatch which main purpose is to support Kinesis listeners customisation.
 */
@Factory
@Requires(classes = AmazonCloudWatch.class)
public class CloudWatchFactory {

    @Bean
    @Singleton
    AmazonCloudWatch cloudWatch(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider
    ) {
        return AmazonCloudWatchClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsRegionProvider.getRegion())
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

}
