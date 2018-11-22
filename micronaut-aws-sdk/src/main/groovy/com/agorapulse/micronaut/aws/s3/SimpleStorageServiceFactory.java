package com.agorapulse.micronaut.aws.s3;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.*;

import javax.inject.Named;
import javax.inject.Singleton;

@Factory
@Requires(classes = AmazonS3.class)
public class SimpleStorageServiceFactory {

    @Bean
    @Singleton
    AmazonS3 amazonS3(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider
    ) {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsRegionProvider.getRegion())
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

    @EachBean(SimpleStorageServiceConfiguration.class)
    @Requires(property = "aws.s3.buckets")
    SimpleStorageService simpleStorageService(AmazonS3 s3, SimpleStorageServiceConfiguration configuration) {
        return new SimpleStorageService(s3, configuration.getBucket());
    }

    @Bean
    @Singleton
    @Named("default")
    @Requires(property = "aws.s3.bucket")
    SimpleStorageService defaultSimpleStorageService(AmazonS3 s3, @Value("aws.s3.bucket") String bucket) {
        return new SimpleStorageService(s3, bucket);
    }

}
