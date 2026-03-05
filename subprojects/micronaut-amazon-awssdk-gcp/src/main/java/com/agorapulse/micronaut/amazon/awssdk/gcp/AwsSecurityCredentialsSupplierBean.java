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

import com.google.auth.oauth2.AwsSecurityCredentials;
import com.google.auth.oauth2.AwsSecurityCredentialsSupplier;
import com.google.auth.oauth2.ExternalAccountSupplierContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * AWS Security Credentials Supplier for GCP Workload Identity Federation.
 * <p>
 * This bean bridges AWS credentials to GCP's Workload Identity Federation, allowing
 * GCP services (like Vertex AI) to authenticate using AWS credentials.
 * <p>
 * Uses the standard AWS SDK credentials provider which works across all AWS environments:
 * <ul>
 *   <li>ECS (via container credentials)</li>
 *   <li>Lambda (via environment credentials)</li>
 *   <li>EC2 (via instance metadata)</li>
 *   <li>Local development (via profiles or environment variables)</li>
 * </ul>
 *
 * @see com.google.auth.oauth2.AwsCredentials
 */
@Singleton
public class AwsSecurityCredentialsSupplierBean implements AwsSecurityCredentialsSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AwsSecurityCredentialsSupplierBean.class);

    private final transient AwsCredentialsProvider credentialsProvider;
    private final transient AwsRegionProvider regionProvider;

    public AwsSecurityCredentialsSupplierBean(
            AwsCredentialsProvider credentialsProvider,
            AwsRegionProvider regionProvider
    ) {
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
    }

    @Override
    public AwsSecurityCredentials getCredentials(ExternalAccountSupplierContext context) {
        LOG.debug("Fetching AWS credentials via SDK credentials provider for GCP WIF");
        
        var creds = credentialsProvider.resolveCredentials();
        
        if (creds instanceof AwsSessionCredentials sessionCreds) {
            LOG.debug("Using session credentials with token");
            return new AwsSecurityCredentials(
                    sessionCreds.accessKeyId(),
                    sessionCreds.secretAccessKey(),
                    sessionCreds.sessionToken()
            );
        }
        
        LOG.debug("Using basic credentials without token");
        return new AwsSecurityCredentials(
                creds.accessKeyId(),
                creds.secretAccessKey(),
                null
        );
    }

    @Override
    public String getRegion(ExternalAccountSupplierContext context) {
        return regionProvider.getRegion().id();
    }
}
