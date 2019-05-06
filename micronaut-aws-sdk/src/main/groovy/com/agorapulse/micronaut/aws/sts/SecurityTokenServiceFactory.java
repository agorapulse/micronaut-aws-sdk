package com.agorapulse.micronaut.aws.sts;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import io.micronaut.configuration.aws.AWSClientConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Optional;

@Factory
@Requires(classes = AWSSecurityTokenService.class)
public class SecurityTokenServiceFactory {

    @Bean
    @Singleton
    AWSSecurityTokenService awsSecurityTokenService(
        AWSClientConfiguration clientConfiguration,
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider awsRegionProvider,
        @Value("${aws.sts.region}") Optional<String> region
    ) {
        return AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(region.orElseGet(awsRegionProvider::getRegion))
            .withClientConfiguration(clientConfiguration.getClientConfiguration())
            .build();
    }

}
