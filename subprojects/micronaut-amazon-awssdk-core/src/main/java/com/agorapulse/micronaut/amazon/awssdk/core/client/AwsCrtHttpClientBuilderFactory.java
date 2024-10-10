package com.agorapulse.micronaut.amazon.awssdk.core.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;

@Factory
@Requires(classes = {AwsCrtHttpClient.class, AwsCrtAsyncHttpClient.class})
public class AwsCrtHttpClientBuilderFactory {

    @Bean
    @Singleton
    @Named("aws-crt")
    public SdkAsyncHttpClient.Builder<AwsCrtAsyncHttpClient.Builder> awsCrtHttpAsyncClientBuilder() {
        return AwsCrtAsyncHttpClient.builder();
    }

    @Bean
    @Singleton
    @Named("aws-crt")
    public SdkHttpClient.Builder<AwsCrtHttpClient.Builder> awsCrtHttpClientBuilder() {
        return AwsCrtHttpClient.builder();
    }

}
