package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = KinesisClientLibConfiguration.class)
public class KinesisClientConfigurationFactory {

    @Singleton
    @Requires(property = "aws.kinesis")
    @EachBean(KinesisClientConfiguration.class)
    KinesisClientLibConfiguration kinesisClientLibConfiguration(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        KinesisClientConfiguration configuration,
        @Value("${aws.kinesis.region}") Optional<String> region
    ) {
        return configuration.getKinesisClientLibConfiguration(
            clientConfiguration.getClientConfiguration(),
            credentialsProvider,
            region.orElseGet(awsRegionProvider::getRegion)
        );
    }

}
