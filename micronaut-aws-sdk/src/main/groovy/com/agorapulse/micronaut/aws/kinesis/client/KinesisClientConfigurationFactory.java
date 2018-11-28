package com.agorapulse.micronaut.aws.kinesis.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

@Factory
@Requires(classes = KinesisClientLibConfiguration.class)
public class KinesisClientConfigurationFactory {

    @Requires(property = "aws.kinesis")
    @EachBean(KinesisClientConfiguration.class)
    KinesisClientLibConfiguration kinesisClientLibConfiguration(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        KinesisClientConfiguration configuration
    ) {
        return configuration.getKinesisClientLibConfiguration(clientConfiguration.getClientConfiguration(), credentialsProvider, awsRegionProvider.getRegion());
    }

}
