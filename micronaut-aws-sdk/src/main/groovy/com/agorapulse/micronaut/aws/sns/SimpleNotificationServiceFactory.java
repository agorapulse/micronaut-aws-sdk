package com.agorapulse.micronaut.aws.sns;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AmazonSNS.class)
public class SimpleNotificationServiceFactory {

    @Bean
    @Singleton
    AmazonSNS amazonSNS(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.sns.region}") Optional<String> region
    ) {
        return AmazonSNSClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(SimpleNotificationServiceConfiguration.class)
    SimpleNotificationService simpleQueueService(AmazonSNS sqs, SimpleNotificationServiceConfiguration configuration, ObjectMapper mapper) {
        return new DefaultSimpleNotificationService(sqs, configuration, mapper);
    }

}
