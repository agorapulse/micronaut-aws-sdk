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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import com.agorapulse.micronaut.amazon.awssdk.core.RegionAndEndpointConfiguration;

import io.micronaut.core.annotation.Nullable;

/**
 * Default configuration for Simple Queue Service.
 */
public abstract class SimpleQueueServiceConfiguration extends QueueConfiguration implements RegionAndEndpointConfiguration {

    private String queueNamePrefix = "";
    private boolean autoCreateQueue;
    private boolean cache;

    @Nullable private String region;
    @Nullable private String endpoint;
    @Nullable private String client;
    @Nullable private String asyncClient;

    public String getQueueNamePrefix() {
        return queueNamePrefix;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    public boolean isAutoCreateQueue() {
        return autoCreateQueue;
    }

    public void setAutoCreateQueue(boolean autoCreateQueue) {
        this.autoCreateQueue = autoCreateQueue;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public @Nullable String getClient() {
        return client;
    }

    public void setClient(@Nullable String client) {
        this.client = client;
    }

    @Override
    public @Nullable String getAsyncClient() {
        return asyncClient;
    }

    public void setAsyncClient(@Nullable String asyncClient) {
        this.asyncClient = asyncClient;
    }
}
