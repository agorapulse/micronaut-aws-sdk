/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

import java.util.function.Consumer;

public interface SecurityTokenService {

    default AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds) {
        return assumeRole(sessionName, roleArn, durationInSeconds, p -> { });
    }

    default AssumeRoleResult assumeRole(
        String sessionName,
        String roleArn,
        int durationInSeconds,
        @DelegatesTo(value = AssumeRoleRequest.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = SimpleType.class, options = "com.amazonaws.services.securitytoken.model.AssumeRoleRequest")
            Closure additionParameters
    ) {
        return assumeRole(sessionName, roleArn, durationInSeconds, ConsumerWithDelegate.create(additionParameters));
    }

    /**
     * Create credentials for role assumption.
     * @param sessionName the name of the session
     * @param roleArn the role ARN
     * @param durationInSeconds the duration in seconds
     * @param additionParameters the additional parameters
     * @return the assumption result containing the provisioned credentials
     */
    AssumeRoleResult assumeRole(String sessionName, String roleArn, int durationInSeconds, Consumer<AssumeRoleRequest> additionParameters);

}
