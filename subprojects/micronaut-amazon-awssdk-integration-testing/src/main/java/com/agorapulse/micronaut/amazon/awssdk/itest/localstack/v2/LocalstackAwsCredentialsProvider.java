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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack.v2;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;

import javax.inject.Singleton;

@Primary
@Singleton
@Replaces(AwsCredentialsProviderChain.class)
public class LocalstackAwsCredentialsProvider implements AwsCredentialsProvider {

    @Override
    public AwsCredentials resolveCredentials() {
        return AwsBasicCredentials.create(
            "accesskey", "secretkey"
        );
    }

}
