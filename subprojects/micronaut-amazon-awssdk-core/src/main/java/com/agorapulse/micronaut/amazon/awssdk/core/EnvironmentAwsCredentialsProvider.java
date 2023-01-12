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
package com.agorapulse.micronaut.amazon.awssdk.core;

import io.micronaut.context.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A {@link AwsCredentialsProvider} that reads from the {@link Environment}.
 */
public class EnvironmentAwsCredentialsProvider implements AwsCredentialsProvider {

    /**
     * Environment variable name for the AWS access key ID.
     */
    public static final String ACCESS_KEY_ENV_VAR = "aws.access-key-id";

    /**
     * Alternate environment variable name for the AWS access key ID.
     */
    public static final String ALTERNATE_ACCESS_KEY_ENV_VAR = "aws.access-key";

    /**
     * Environment variable name for the AWS secret key.
     */
    public static final String SECRET_KEY_ENV_VAR = "aws.secret-key";

    /**
     * Alternate environment variable name for the AWS secret key.
     */
    public static final String ALTERNATE_SECRET_KEY_ENV_VAR = "aws.secret-access-key";

    /**
     * Environment variable name for the AWS session token.
     */
    public static final String AWS_SESSION_TOKEN_ENV_VAR = "aws.session-token";

    public static AwsCredentialsProvider create(Environment environment) {
        return new EnvironmentAwsCredentialsProvider(environment);
    }

    private final Environment environment;

    /**
     * Constructor.
     * @param environment environment
     */
    private EnvironmentAwsCredentialsProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        String accessKey = environment.getProperty(ACCESS_KEY_ENV_VAR, String.class, environment.getProperty(ALTERNATE_ACCESS_KEY_ENV_VAR, String.class, (String) null));

        String secretKey = environment.getProperty(SECRET_KEY_ENV_VAR, String.class, environment.getProperty(ALTERNATE_SECRET_KEY_ENV_VAR, String.class, (String) null));
        accessKey = StringUtils.trim(accessKey);
        secretKey = StringUtils.trim(secretKey);
        String sessionToken = StringUtils.trim(environment.getProperty(AWS_SESSION_TOKEN_ENV_VAR, String.class, (String) null));

        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
            throw SdkClientException.create(
                "Unable to load AWS credentials from environment "
                    + "(" + ACCESS_KEY_ENV_VAR + " (or " + ALTERNATE_ACCESS_KEY_ENV_VAR + ") and "
                    + SECRET_KEY_ENV_VAR + " (or " + ALTERNATE_SECRET_KEY_ENV_VAR + "))");
        }

        return sessionToken == null
            ? AwsBasicCredentials.create(accessKey, secretKey)
            : AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
