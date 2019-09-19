package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonKinesis.class)
public class KinesisFactory {

    @Singleton
    @EachBean(KinesisConfiguration.class)
    AmazonKinesis kinesis(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        KinesisConfiguration configuration
    ) {
        return configuration.configure(AmazonKinesisClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Singleton
    @EachBean(KinesisConfiguration.class)
    KinesisService simpleQueueService(
        AmazonKinesis kinesis,
        KinesisConfiguration configuration,
        ObjectMapper mapper
    ) {
        return new DefaultKinesisService(kinesis, configuration, mapper);
    }

}
