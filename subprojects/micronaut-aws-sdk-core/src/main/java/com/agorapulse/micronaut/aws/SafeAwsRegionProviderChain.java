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

import com.amazonaws.regions.AwsRegionProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Region provider chain which throws no exception but returns null instead.
 */
public class SafeAwsRegionProviderChain extends AwsRegionProvider {

    private static final Log LOG = LogFactory.getLog(SafeAwsRegionProviderChain.class);

    private final List<AwsRegionProvider> providers;

    public SafeAwsRegionProviderChain(AwsRegionProvider... providers) {
        this.providers = new ArrayList<>(providers.length);
        Collections.addAll(this.providers, providers);
    }

    @Override
    public String getRegion(){
        for (AwsRegionProvider provider : providers) {
            try {
                final String region = provider.getRegion();
                if (region != null) {
                    return region;
                }
            } catch (Exception e) {
                // Ignore any exceptions and move onto the next provider
                LOG.debug("Unable to load region from " + provider.toString() + ": " + e.getMessage());
            }
        }
        return null;
    }
}
