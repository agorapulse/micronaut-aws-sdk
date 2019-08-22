package com.agorapulse.micronaut.aws

import com.agorapulse.micronaut.aws.RegionAndEndpointConfiguration

import javax.annotation.Nullable

/**
 * Default region and endpoint configuration.
 */
class DefaultRegionAndEndpointConfiguration implements RegionAndEndpointConfiguration {

    @Nullable String region
    @Nullable String endpoint

}
