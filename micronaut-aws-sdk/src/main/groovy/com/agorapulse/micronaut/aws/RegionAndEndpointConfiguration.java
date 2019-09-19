package com.agorapulse.micronaut.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.AwsRegionProvider;

import java.util.Optional;

public interface RegionAndEndpointConfiguration {

    String getRegion();

    String getEndpoint();

    default <C, B extends AwsClientBuilder<B, C>> B configure(B builder, AwsRegionProvider awsRegionProvider) {
        String region = Optional.ofNullable(getRegion()).orElseGet(awsRegionProvider::getRegion);
        if (getEndpoint() != null) {
            builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getEndpoint(), region));
        } else {
            builder.setRegion(region);
        }
        return builder;
    }

}
