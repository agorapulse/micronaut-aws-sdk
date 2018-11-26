package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonKinesis.class)
public class KinesisFactory {

    @Bean
    @Singleton
    AmazonKinesis kinesis(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider
    ) {
        return AmazonKinesisClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsRegionProvider.getRegion())
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(KinesisConfiguration.class)
    KinesisService simpleQueueService(AmazonKinesis kinesis, KinesisConfiguration configuration, ObjectMapper mapper) {
        return new KinesisService(kinesis, configuration, mapper);
    }

}
