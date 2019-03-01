package com.agorapulse.micronaut.aws.sqs;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AmazonSQS.class)
public class SimpleQueueServiceFactory {

    @Bean
    @Singleton
    AmazonSQS amazonSQS(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.sqs.region}") Optional<String> region
    ) {
        return AmazonSQSClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(SimpleQueueServiceConfiguration.class)
    SimpleQueueService simpleQueueService(AmazonSQS sqs, SimpleQueueServiceConfiguration configuration) {
        return new DefaultSimpleQueueService(sqs, configuration);
    }

}
