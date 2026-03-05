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

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

/**
 * Configuration for GCP Workload Identity Federation with AWS.
 * <p>
 * Uses the same environment variables as typically configured in ECS task definitions:
 * <ul>
 *   <li>{@code GCP_PROJECT_NUMBER} - The GCP project number (not project ID)</li>
 *   <li>{@code GCP_WORKLOAD_POOL} - The Workload Identity Pool ID</li>
 *   <li>{@code GCP_WORKLOAD_PROVIDER} - The Workload Identity Provider ID</li>
 *   <li>{@code GCP_SERVICE_ACCOUNT} - The GCP service account email to impersonate</li>
 * </ul>
 */
@Singleton
public class GcpWorkloadIdentityConfiguration {

    private final String projectNumber;
    private final String workloadPool;
    private final String workloadProvider;
    private final String serviceAccount;

    public GcpWorkloadIdentityConfiguration(
            @Value("${GCP_PROJECT_NUMBER:}") String projectNumber,
            @Value("${GCP_WORKLOAD_POOL:}") String workloadPool,
            @Value("${GCP_WORKLOAD_PROVIDER:}") String workloadProvider,
            @Value("${GCP_SERVICE_ACCOUNT:}") String serviceAccount
    ) {
        this.projectNumber = projectNumber;
        this.workloadPool = workloadPool;
        this.workloadProvider = workloadProvider;
        this.serviceAccount = serviceAccount;
    }

    /**
     * @return the GCP project number (not project ID)
     */
    public String getProjectNumber() {
        return projectNumber;
    }

    /**
     * @return the Workload Identity Pool ID
     */
    public String getWorkloadPool() {
        return workloadPool;
    }

    /**
     * @return the Workload Identity Provider ID
     */
    public String getWorkloadProvider() {
        return workloadProvider;
    }

    /**
     * @return the GCP service account email to impersonate
     */
    public String getServiceAccount() {
        return serviceAccount;
    }

    /**
     * @return true if all required properties are configured
     */
    public boolean isFullyConfigured() {
        return isNotEmpty(projectNumber)
                && isNotEmpty(workloadPool)
                && isNotEmpty(workloadProvider)
                && isNotEmpty(serviceAccount);
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
