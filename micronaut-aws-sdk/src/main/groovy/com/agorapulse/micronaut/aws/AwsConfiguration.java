package com.agorapulse.micronaut.aws;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.*;
import io.micronaut.configuration.aws.EnvironmentAWSCredentialsProvider;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;

import javax.inject.Singleton;

@Factory
public class AwsConfiguration {

    private static final Regions DEFAULT_REGION = Regions.EU_WEST_1;

    @Bean
    @Singleton
    AWSCredentialsProvider defaultAwsCredentialsProvider(Environment environment) {
        return new AWSCredentialsProviderChain(
            new EnvironmentAWSCredentialsProvider(environment),
            new EnvironmentVariableCredentialsProvider(),
            new SystemPropertiesCredentialsProvider(),
            new ProfileCredentialsProvider(),
            new EC2ContainerCredentialsProviderWrapper()
        );
    }

    @Bean
    @Singleton
    AwsRegionProvider defaultAwsRegionProvider(Environment environment) {
        return new AwsRegionProviderChain(
            new EnvironmentAwsRegionProvider(environment),
            new AwsEnvVarOverrideRegionProvider(),
            new AwsSystemPropertyRegionProvider(),
            new AwsProfileRegionProvider(),
            new InstanceMetadataRegionProvider(),
            new BasicAwsRegionProvider(DEFAULT_REGION.getName())
        );
    }

}
