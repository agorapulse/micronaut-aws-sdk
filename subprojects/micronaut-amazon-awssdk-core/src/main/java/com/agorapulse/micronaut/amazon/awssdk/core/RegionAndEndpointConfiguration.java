/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.core;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public interface RegionAndEndpointConfiguration {

    String getRegion();

    String getEndpoint();

    default <C, B extends AwsClientBuilder<B, C>> B configure(B builder, AwsRegionProvider awsRegionProvider) {
        Region region = Optional.ofNullable(getRegion()).map(Region::of).orElseGet(awsRegionProvider::getRegion);
        if (getEndpoint() != null) {
            try {
                builder.endpointOverride(new URI(getEndpoint()));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Endpoint configured for " + getClass() + " is not a valid URI!", e);
            }
        } else {
            builder.region(region);
        }
        return builder;
    }

}
