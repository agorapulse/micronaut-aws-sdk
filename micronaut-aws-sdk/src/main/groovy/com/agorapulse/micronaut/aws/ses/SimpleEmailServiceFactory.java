package com.agorapulse.micronaut.aws.ses;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AmazonSimpleEmailService.class)
public class SimpleEmailServiceFactory {

    @Bean
    @Singleton
    AmazonSimpleEmailService amazonSimpleEmailService(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.ses.region}") Optional<String> region
    ) {
        return AmazonSimpleEmailServiceClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

}
