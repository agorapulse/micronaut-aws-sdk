package com.agorapulse.micronaut.amazon.awssdk.localstack;

import com.agorapulse.micronaut.amazon.awssdk.core.AwsConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;

import javax.inject.Singleton;

@Factory
@Replaces(AwsConfiguration.class)
public class LocalstackContainerHolderFactory {

    @Singleton
    @Bean(preDestroy = "close")
    public LocalstackContainerHolder localstackContainerHolder(LocalstackContainerConfiguration configuration) {
        return new LocalstackContainerHolder(configuration);
    }

}
