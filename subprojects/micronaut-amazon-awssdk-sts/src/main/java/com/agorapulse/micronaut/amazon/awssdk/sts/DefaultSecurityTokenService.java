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
package com.agorapulse.micronaut.amazon.awssdk.sts;

import io.micronaut.context.annotation.Requires;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

import jakarta.inject.Singleton;
import java.util.function.Consumer;

@Singleton
@Requires(classes = StsClient.class)
public class DefaultSecurityTokenService implements SecurityTokenService {

    private final StsClient client;

    public DefaultSecurityTokenService(StsClient client) {
        this.client = client;
    }

    @Override
    public AssumeRoleResponse assumeRole(String sessionName, String roleArn, int durationInSeconds, Consumer<AssumeRoleRequest.Builder> additionParameters) {
        AssumeRoleRequest.Builder request = AssumeRoleRequest.builder()
            .roleSessionName(sessionName)
            .roleArn(roleArn)
            .durationSeconds(durationInSeconds);

        additionParameters.accept(request);

        return client.assumeRole(request.build());
    }
}
