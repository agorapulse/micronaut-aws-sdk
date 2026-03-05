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

import com.google.auth.oauth2.AwsCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating GCP credentials using AWS Workload Identity Federation.
 * <p>
 * This factory creates {@link GoogleCredentials} that use AWS credentials
 * to authenticate with GCP services via Workload Identity Federation.
 * <p>
 * The credentials are only created when:
 * <ul>
 *   <li>The configuration is enabled ({@code gcp.workload-identity.enabled=true})</li>
 *   <li>All required properties are configured (project-number, pool-id, provider-id, service-account-email)</li>
 * </ul>
 */
@Factory
public class GcpCredentialsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GcpCredentialsFactory.class);

    /**
     * Creates GCP credentials using AWS Workload Identity Federation.
     *
     * @param configuration the WIF configuration
     * @param credentialsSupplier the AWS credentials supplier
     * @return GoogleCredentials configured for AWS WIF
     */
    @Bean
    @Singleton
    @Requires(beans = {GcpWorkloadIdentityConfiguration.class, AwsSecurityCredentialsSupplierBean.class})
    @Requires(property = "gcp.workload-identity.enabled", value = "true", defaultValue = "true")
    @Requires(property = "gcp.workload-identity.project-number")
    @Requires(property = "gcp.workload-identity.pool-id")
    @Requires(property = "gcp.workload-identity.provider-id")
    @Requires(property = "gcp.workload-identity.service-account-email")
    public GoogleCredentials gcpCredentials(
            GcpWorkloadIdentityConfiguration configuration,
            AwsSecurityCredentialsSupplierBean credentialsSupplier
    ) {
        LOG.info("Creating GCP credentials with AWS Workload Identity Federation");
        LOG.debug("Project number: {}, Pool: {}, Provider: {}",
                configuration.getProjectNumber(),
                configuration.getPoolId(),
                configuration.getProviderId());

        String audience = String.format(
                "//iam.googleapis.com/projects/%s/locations/global/workloadIdentityPools/%s/providers/%s",
                configuration.getProjectNumber(),
                configuration.getPoolId(),
                configuration.getProviderId()
        );

        String serviceAccountImpersonationUrl = String.format(
                "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:generateAccessToken",
                configuration.getServiceAccountEmail()
        );

        LOG.debug("Audience: {}", audience);
        LOG.debug("Service account impersonation URL: {}", serviceAccountImpersonationUrl);

        return AwsCredentials.newBuilder()
                .setAwsSecurityCredentialsSupplier(credentialsSupplier)
                .setAudience(audience)
                .setSubjectTokenType("urn:ietf:params:aws:token-type:aws4_request")
                .setTokenUrl("https://sts.googleapis.com/v1/token")
                .setServiceAccountImpersonationUrl(serviceAccountImpersonationUrl)
                .build();
    }
}
