package com.agorapulse.micronaut.aws.sqs;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonSQS.class)
public class SimpleQueueServiceFactory {

    @Bean
    @Singleton
    AmazonSQS amazonSQS(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleQueueServiceConfiguration configuration
    ) {
        return configuration.configure(AmazonSQSClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(SimpleQueueServiceConfiguration.class)
    SimpleQueueService simpleQueueService(AmazonSQS sqs, SimpleQueueServiceConfiguration configuration) {
        return new DefaultSimpleQueueService(sqs, configuration);
    }

}
