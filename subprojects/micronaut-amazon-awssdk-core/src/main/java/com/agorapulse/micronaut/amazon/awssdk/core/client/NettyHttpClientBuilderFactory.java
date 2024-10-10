package com.agorapulse.micronaut.amazon.awssdk.core.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

@Factory
@Requires(classes = NettyNioAsyncHttpClient.class)
public class NettyHttpClientBuilderFactory {

    @Bean
    @Singleton
    @Named("netty")
    public SdkAsyncHttpClient.Builder<NettyNioAsyncHttpClient.Builder> nettyHttpAsyncClientBuilder() {
        return NettyNioAsyncHttpClient.builder();
    }

}
