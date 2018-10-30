package com.agorapulse.micronaut.aws;

import com.amazonaws.regions.AwsRegionProvider;

public class BasicAwsRegionProvider extends AwsRegionProvider {

    private final String region;

    public BasicAwsRegionProvider(String region) {
        this.region = region;
    }

    @Override
    public String getRegion() {
        return region;
    }
}
