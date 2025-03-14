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

import io.micronaut.core.annotation.Nullable;

/**
 * Default region and endpoint configuration.
 */
public class DefaultRegionAndEndpointConfiguration implements RegionAndEndpointConfiguration {

    @Nullable
    private String region;
    @Nullable
    private String endpoint;

    @Nullable
    public String getRegion() {
        return region;
    }

    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(@Nullable String endpoint) {
        this.endpoint = endpoint;
    }
}
