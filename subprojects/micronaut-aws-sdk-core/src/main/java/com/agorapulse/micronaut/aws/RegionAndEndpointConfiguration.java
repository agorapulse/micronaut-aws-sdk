/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
