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
package com.agorapulse.micronaut.amazon.awssdk.gcp;

import com.agorapulse.micronaut.amazon.awssdk.core.util.ConfigurationUtil;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;

/**
 * Named GCP Workload Identity Federation configuration created for each entry under
 * {@code gcp.credentials}. The entry named {@code default} becomes the primary bean.
 */
@Requires(property = "gcp.credentials")
@EachProperty(value = "gcp.credentials", primary = ConfigurationUtil.DEFAULT_CONFIGURATION_NAME)
public class NamedGcpWorkloadIdentityConfiguration extends GcpWorkloadIdentityConfiguration {

    private final String name;

    public NamedGcpWorkloadIdentityConfiguration(@Parameter String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
