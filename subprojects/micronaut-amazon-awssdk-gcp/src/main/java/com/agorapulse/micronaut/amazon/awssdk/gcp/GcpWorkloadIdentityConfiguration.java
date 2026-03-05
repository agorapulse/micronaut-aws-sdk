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

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration for GCP Workload Identity Federation with AWS.
 * <p>
 * Configure these properties to enable automatic creation of GCP credentials
 * using AWS credentials via Workload Identity Federation.
 * <p>
 * Example configuration:
 * <pre>
 * gcp:
 *   workload-identity:
 *     enabled: true
 *     project-number: "123456789012"
 *     pool-id: "aws-pool"
 *     provider-id: "aws-provider"
 *     service-account-email: "my-sa@project.iam.gserviceaccount.com"
 * </pre>
 */
@ConfigurationProperties("gcp.workload-identity")
public class GcpWorkloadIdentityConfiguration {

    private boolean enabled = true;
    private String projectNumber;
    private String poolId;
    private String providerId;
    private String serviceAccountEmail;

    /**
     * @return whether GCP Workload Identity Federation is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled whether GCP Workload Identity Federation is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the GCP project number (not project ID)
     */
    public String getProjectNumber() {
        return projectNumber;
    }

    /**
     * @param projectNumber the GCP project number (not project ID)
     */
    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    /**
     * @return the Workload Identity Pool ID
     */
    public String getPoolId() {
        return poolId;
    }

    /**
     * @param poolId the Workload Identity Pool ID
     */
    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    /**
     * @return the Workload Identity Provider ID
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the Workload Identity Provider ID
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * @return the GCP service account email to impersonate
     */
    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    /**
     * @param serviceAccountEmail the GCP service account email to impersonate
     */
    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    /**
     * @return true if all required properties are configured
     */
    public boolean isFullyConfigured() {
        return projectNumber != null && !projectNumber.isEmpty()
                && poolId != null && !poolId.isEmpty()
                && providerId != null && !providerId.isEmpty()
                && serviceAccountEmail != null && !serviceAccountEmail.isEmpty();
    }
}
