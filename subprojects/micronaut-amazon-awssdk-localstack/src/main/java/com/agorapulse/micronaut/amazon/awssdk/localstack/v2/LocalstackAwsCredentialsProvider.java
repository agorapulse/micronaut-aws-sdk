package com.agorapulse.micronaut.amazon.awssdk.localstack.v2;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;

import javax.inject.Singleton;

@Primary
@Singleton
@Replaces(AwsCredentialsProviderChain.class)
public class LocalstackAwsCredentialsProvider implements AwsCredentialsProvider {

    private final LocalstackContainerHolder holder;

    public LocalstackAwsCredentialsProvider(LocalstackContainerHolder holder) {
        this.holder = holder;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        LocalStackContainer localstack = holder.getRunningContainer();
        return AwsBasicCredentials.create(
            localstack.getAccessKey(), localstack.getSecretKey()
        );
    }

}
