package com.agorapulse.micronaut.amazon.awssdk.core.client;

import io.micronaut.context.annotation.Bean;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

import java.util.Map;
import java.util.Optional;

@Bean
public class DefaultClientBuilderProvider implements ClientBuilderProvider {

    private final Map<String, SdkHttpClient.Builder<?>> httpClientBuilders;
    private final Map<String, SdkAsyncHttpClient.Builder<?>> httpAsyncClientBuilders;

    public DefaultClientBuilderProvider(Map<String, SdkHttpClient.Builder<?>> httpClientBuilders, Map<String, SdkAsyncHttpClient.Builder<?>> httpAsyncClientBuilders) {
        this.httpClientBuilders = httpClientBuilders;
        this.httpAsyncClientBuilders = httpAsyncClientBuilders;
    }

    @Override
    public <B extends SdkHttpClient.Builder<B>> Optional<SdkHttpClient.Builder<B>> findHttpClientBuilder(String implementation) {
        return Optional.ofNullable((SdkHttpClient.Builder<B>) httpClientBuilders.get(implementation));
    }

    @Override
    public <B extends SdkAsyncHttpClient.Builder<B>> Optional<SdkAsyncHttpClient.Builder<B>> findAsyncHttpClientBuilder(String implementation) {
        return Optional.ofNullable((SdkAsyncHttpClient.Builder<B>) httpAsyncClientBuilders.get(implementation));
    }

}
