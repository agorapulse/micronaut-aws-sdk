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
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

@Factory
@Requires(classes = UrlConnectionHttpClient.class)
public class UrlConnectionHttpClientBuilderFactory {

    @Bean
    @Singleton
    @Named("url-connection")
    public SdkHttpClient.Builder<UrlConnectionHttpClient.Builder> awsCrtHttpClientBuilder() {
        return UrlConnectionHttpClient.builder();
    }

}
