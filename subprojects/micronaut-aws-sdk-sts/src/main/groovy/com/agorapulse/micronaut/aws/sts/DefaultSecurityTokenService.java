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
package com.agorapulse.micronaut.aws.sts;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import io.micronaut.context.annotation.Requires;

import javax.inject.Singleton;
import java.util.function.Consumer;

@Singleton
@Requires(classes = AWSSecurityTokenService.class)
public class DefaultSecurityTokenService implements SecurityTokenService {

    private final AWSSecurityTokenService client;

    public DefaultSecurityTokenService(AWSSecurityTokenService client) {
        this.client = client;
    }

    @Override
    public AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds, Consumer<AssumeRoleRequest> additionParameters) {
        AssumeRoleRequest request = new AssumeRoleRequest()
            .withRoleSessionName(sessionName)
            .withRoleArn(roleArn)
            .withDurationSeconds(durationInSeconds);

        additionParameters.accept(request);

        return client.assumeRole(request);
    }
}
