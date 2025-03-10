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
package com.agorapulse.micronaut.amazon.awssdk.s3;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;

/**
 * Simple storage service configuration for each configuration key.
 */
@EachProperty(value = "aws.s3.buckets", primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
public class NamedSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

    public NamedSimpleStorageServiceConfiguration(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private String name;

}
