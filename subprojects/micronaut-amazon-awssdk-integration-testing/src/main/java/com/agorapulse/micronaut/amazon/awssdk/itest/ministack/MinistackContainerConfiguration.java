/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.ministack;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("ministack")
public class MinistackContainerConfiguration {

    private String tag;
    private boolean shared;
    private String region;
    private String accessKey;
    private String secretKey;
    private boolean persistence;
    private boolean imdsV2Required;
    private boolean realInfrastructure;
    private Map<String, String> env = new HashMap<>();

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPersistence() {
        return persistence;
    }

    public void setPersistence(boolean persistence) {
        this.persistence = persistence;
    }

    public boolean isImdsV2Required() {
        return imdsV2Required;
    }

    public void setImdsV2Required(boolean imdsV2Required) {
        this.imdsV2Required = imdsV2Required;
    }

    public boolean isRealInfrastructure() {
        return realInfrastructure;
    }

    public void setRealInfrastructure(boolean realInfrastructure) {
        this.realInfrastructure = realInfrastructure;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(@MapFormat(keyFormat = StringConvention.RAW) Map<String, String> env) {
        this.env = env;
    }

}
