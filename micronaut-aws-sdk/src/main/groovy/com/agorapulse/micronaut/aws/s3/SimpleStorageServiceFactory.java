package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AmazonS3.class)
public class SimpleStorageServiceFactory {

    @Bean
    @Singleton
    AmazonS3 amazonS3(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        SimpleStorageServiceConfiguration configuration
    ) {
        return configuration.configure(AmazonS3ClientBuilder.standard(), awsRegionProvider)
            .withCredentials(credentialsProvider)
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(SimpleStorageServiceConfiguration.class)
    SimpleStorageService simpleStorageService(AmazonS3 s3, SimpleStorageServiceConfiguration configuration) {
        return new DefaultSimpleStorageService(s3, configuration.getBucket());
    }

}
