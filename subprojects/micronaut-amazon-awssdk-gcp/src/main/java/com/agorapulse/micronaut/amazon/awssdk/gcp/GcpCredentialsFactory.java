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
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating GCP credentials using AWS Workload Identity Federation.
 * <p>
 * Produces one {@link GoogleCredentials} bean per {@link GcpWorkloadIdentityConfiguration}.
 * With the flat {@code gcp.*} properties this yields a single primary bean; with the
 * {@code gcp.credentials.<name>} layout, one bean is produced per entry, qualified by
 * the entry name (the {@code default} entry is primary).
 */
@Factory
public class GcpCredentialsFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GcpCredentialsFactory.class);

    @Bean
    @Singleton
    @EachBean(GcpWorkloadIdentityConfiguration.class)
    public GoogleCredentials gcpCredentials(
            GcpWorkloadIdentityConfiguration configuration,
            AwsSecurityCredentialsSupplierBean credentialsSupplier
    ) {
        if (!configuration.isFullyConfigured()) {
            throw new IllegalStateException(
                    "GCP credentials configuration is incomplete; project-number, workload-pool, workload-provider and service-account are all required"
            );
        }

        LOG.info("Creating GCP credentials with AWS Workload Identity Federation");
        LOG.debug("Project number: {}, Pool: {}, Provider: {}",
                configuration.getProjectNumber(),
                configuration.getWorkloadPool(),
                configuration.getWorkloadProvider());

        String audience = String.format(
                "//iam.googleapis.com/projects/%s/locations/global/workloadIdentityPools/%s/providers/%s",
                configuration.getProjectNumber(),
                configuration.getWorkloadPool(),
                configuration.getWorkloadProvider()
        );

        String serviceAccountImpersonationUrl = String.format(
                "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:generateAccessToken",
                configuration.getServiceAccount()
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
