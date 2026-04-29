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

/**
 * Base configuration for GCP Workload Identity Federation with AWS.
 * <p>
 * Two layouts are supported:
 * <p>
 * Single (default) credentials via flat properties or environment variables:
 * <pre>
 * gcp:
 *   project-number: "123456789012"
 *   workload-pool: "aws-pool"
 *   workload-provider: "aws-provider"
 *   service-account: "my-sa@project.iam.gserviceaccount.com"
 * </pre>
 * Equivalent environment variables: {@code GCP_PROJECT_NUMBER}, {@code GCP_WORKLOAD_POOL},
 * {@code GCP_WORKLOAD_PROVIDER}, {@code GCP_SERVICE_ACCOUNT}.
 * <p>
 * Named credentials, producing one {@link com.google.auth.oauth2.GoogleCredentials}
 * bean per entry, qualified by the entry name. May coexist with the flat layout
 * above, which provides the primary {@code default} bean; avoid declaring a
 * {@code gcp.credentials.default} entry in that case to prevent two beans
 * competing for the same qualifier.
 * <pre>
 * gcp:
 *   credentials:
 *     analytics:
 *       project-number: "123456789012"
 *       workload-pool: "aws-pool"
 *       workload-provider: "aws-provider"
 *       service-account: "analytics@project.iam.gserviceaccount.com"
 *     billing:
 *       project-number: "987654321098"
 *       workload-pool: "aws-pool"
 *       workload-provider: "aws-provider"
 *       service-account: "billing@project.iam.gserviceaccount.com"
 * </pre>
 */
public abstract class GcpWorkloadIdentityConfiguration {

    private String projectNumber;
    private String workloadPool;
    private String workloadProvider;
    private String serviceAccount;

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
    public String getWorkloadPool() {
        return workloadPool;
    }

    /**
     * @param workloadPool the Workload Identity Pool ID
     */
    public void setWorkloadPool(String workloadPool) {
        this.workloadPool = workloadPool;
    }

    /**
     * @return the Workload Identity Provider ID
     */
    public String getWorkloadProvider() {
        return workloadProvider;
    }

    /**
     * @param workloadProvider the Workload Identity Provider ID
     */
    public void setWorkloadProvider(String workloadProvider) {
        this.workloadProvider = workloadProvider;
    }

    /**
     * @return the GCP service account email to impersonate
     */
    public String getServiceAccount() {
        return serviceAccount;
    }

    /**
     * @param serviceAccount the GCP service account email to impersonate
     */
    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
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
