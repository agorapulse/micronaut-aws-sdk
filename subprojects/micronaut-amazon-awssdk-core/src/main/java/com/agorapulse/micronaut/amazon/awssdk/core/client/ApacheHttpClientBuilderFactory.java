package com.agorapulse.micronaut.amazon.awssdk.core.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

@Factory
@Requires(classes = ApacheHttpClient.class)
public class ApacheHttpClientBuilderFactory {

    @Bean
    @Singleton
    @Named("apache")
    public SdkHttpClient.Builder<ApacheHttpClient.Builder> awsCrtHttpClientBuilder() {
        return ApacheHttpClient.builder();
    }

}
