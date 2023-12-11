/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.s3;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;

import jakarta.validation.constraints.Size;

/**
 * Default simple storage service configuration.
 */
public abstract class SimpleStorageServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * @return <core>true</core> if the path-style access should be used
     */
    public Boolean getForcePathStyle() {
        return forcePathStyle;
    }

    /**
     * @param forcePathStyle <core>true</core> if the path-style access should be used
     */
    public void setForcePathStyle(Boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }

    /**
     * @return <core>true</core> if the path-style access should be used
     * @deprecated use {@link #getForcePathStyle()} instead
     */
    @Deprecated
    public Boolean getPathStyleAccessEnabled() {
        return forcePathStyle;
    }

    /**
     *
     * @param forcePathStyle <core>true</core> if the path-style access should be used
     * @deprecated use {@link #setForcePathStyle(Boolean)} instead
     */
    @Deprecated
    public void setPathStyleAccessEnabled(Boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }

    @Size(min = 1)
    private String bucket = "";
    private Boolean forcePathStyle;

}
