/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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


import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.util.StringUtils;

@EachProperty("localstack.containers")
public class LocalstackContainerOverridesConfiguration extends AbstractContainerConfiguration {

    private final String service;
    private int port;

    public LocalstackContainerOverridesConfiguration(@Parameter String name) {
        this.service = name;
    }

    public String getService() {
        return service;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isValid() {
        return StringUtils.isNotEmpty(getImage()) && StringUtils.isNotEmpty(getTag()) && getPort() > 0;
    }
 }
