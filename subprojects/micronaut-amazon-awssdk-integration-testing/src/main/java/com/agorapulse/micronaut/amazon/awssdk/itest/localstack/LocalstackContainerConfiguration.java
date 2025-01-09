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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("localstack")
public class LocalstackContainerConfiguration extends AbstractContainerConfiguration {

    public static final Map<String, String> ALWAYS_PRESENT_ENV = Map.of(
        "SQS_ENDPOINT_STRATEGY", "dynamic"
    );

    public static final String DEFAULT_IMAGE = "localstack/localstack";
    public static final String DEFAULT_TAG = "3.2.0";

    public LocalstackContainerConfiguration() {
        setImage(DEFAULT_IMAGE);
        setTag(DEFAULT_TAG);
    }

    private List<String> services = new ArrayList<>();

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    @Override
    public Map<String, String> getEnv() {
        Map<String, String> allEnv = new HashMap<>(super.getEnv());
        ALWAYS_PRESENT_ENV.forEach(allEnv::putIfAbsent);
        return allEnv;
    }
}
