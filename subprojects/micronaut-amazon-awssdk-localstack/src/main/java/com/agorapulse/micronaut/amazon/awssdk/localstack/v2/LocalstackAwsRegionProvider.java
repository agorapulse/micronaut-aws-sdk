package com.agorapulse.micronaut.amazon.awssdk.localstack.v2;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;

import javax.inject.Singleton;

@Primary
@Singleton
@Replaces(AwsRegionProviderChain.class)
public class LocalstackAwsRegionProvider implements AwsRegionProvider {

    private final LocalstackContainerHolder holder;

    public LocalstackAwsRegionProvider(LocalstackContainerHolder holder) {
        this.holder = holder;
    }

    @Override
    public Region getRegion() {
        return Region.of(holder.getRunningContainer().getRegion());
    }

}
