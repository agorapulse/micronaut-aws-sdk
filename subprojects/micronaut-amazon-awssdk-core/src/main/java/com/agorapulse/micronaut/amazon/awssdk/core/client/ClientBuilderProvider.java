package com.agorapulse.micronaut.amazon.awssdk.core.client;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

import java.util.Optional;

public interface ClientBuilderProvider {

    String APACHE = "apache";
    String AWS_CRT = "aws-crt";
    String URL_CONNECTION = "url-connection";
    String NETTY = "netty";

    <B extends SdkHttpClient.Builder<B>> Optional<SdkHttpClient.Builder<B>> findHttpClientBuilder(String implementation);

    <B extends SdkAsyncHttpClient.Builder<B>> Optional<SdkAsyncHttpClient.Builder<B>> findAsyncHttpClientBuilder(String implementation);

}
