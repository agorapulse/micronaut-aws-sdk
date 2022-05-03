package com.agorapulse.micronaut.amazon.awssdk.localstack;

import com.agorapulse.micronaut.amazon.awssdk.core.AwsConfiguration;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

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

    @Primary
    @Singleton
    @Bean(preDestroy = "close")
    public SdkAsyncHttpClient client() {
        return  NettyNioAsyncHttpClient
            .builder()
            .protocol(Protocol.HTTP1_1)
            .buildWithDefaults(
                AttributeMap
                    .builder()
                    .put(
                        SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES,
                        Boolean.TRUE
                    )
                    .build()
            );
    }

}
