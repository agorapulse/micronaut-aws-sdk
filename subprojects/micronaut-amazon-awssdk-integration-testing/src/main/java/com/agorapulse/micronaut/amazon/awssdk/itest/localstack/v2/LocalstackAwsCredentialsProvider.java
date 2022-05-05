/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.itest.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;

import javax.inject.Singleton;

@Primary
@Singleton
@Replaces(AwsCredentialsProviderChain.class)
public class LocalstackAwsCredentialsProvider implements AwsCredentialsProvider {

    private final LocalstackContainerHolder holder;

    public LocalstackAwsCredentialsProvider(LocalstackContainerHolder holder) {
        this.holder = holder;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        LocalStackContainer localstack = holder.requireRunningContainer();
        return AwsBasicCredentials.create(
            localstack.getAccessKey(), localstack.getSecretKey()
        );
    }

}
