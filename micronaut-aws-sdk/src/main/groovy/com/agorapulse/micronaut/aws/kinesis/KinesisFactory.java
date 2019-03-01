package com.agorapulse.micronaut.aws.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AmazonKinesis.class)
public class KinesisFactory {

    @Bean
    @Singleton
    AmazonKinesis kinesis(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.kinesis.region}") Optional<String> region
    ) {
        return AmazonKinesisClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(KinesisConfiguration.class)
    KinesisService simpleQueueService(AmazonKinesis kinesis, KinesisConfiguration configuration, ObjectMapper mapper) {
        return new DefaultKinesisService(kinesis, configuration, mapper);
    }

}
