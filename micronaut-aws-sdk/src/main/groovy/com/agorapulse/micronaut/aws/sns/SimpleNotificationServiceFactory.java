package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonSNS.class)
public class SimpleNotificationServiceFactory {

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    AmazonSNS amazonSNS(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleNotificationServiceConfiguration configuration
    ) {
        return configuration.configure(AmazonSNSClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @Singleton
    @EachBean(SimpleNotificationServiceConfiguration.class)
    SimpleNotificationService simpleQueueService(AmazonSNS sqs, SimpleNotificationServiceConfiguration configuration, ObjectMapper mapper) {
        return new DefaultSimpleNotificationService(sqs, configuration, mapper);
    }

}
