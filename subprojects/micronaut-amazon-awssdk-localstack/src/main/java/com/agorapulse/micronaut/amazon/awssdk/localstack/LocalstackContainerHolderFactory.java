package com.agorapulse.micronaut.amazon.awssdk.localstack;

import com.agorapulse.micronaut.amazon.awssdk.core.AwsConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;

@Factory
@Replaces(AwsConfiguration.class)
@Requires(missingProperty = "localstack.disabled")
public class LocalstackContainerHolderFactory {

    @Primary
    @Singleton
    @Bean(preDestroy = "close")
    public LocalstackContainerHolder localstackContainerHolder(LocalstackContainerConfiguration configuration) {
        return new LocalstackContainerHolder(configuration);
    }

}
