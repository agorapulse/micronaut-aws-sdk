/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.aws.sts

import groovy.transform.CompileDynamic
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * Tests for SecurityTokenService.
 */
@CompileDynamic
class SecurityTokenServiceSpec extends Specification {

    public static final String SESSION_NAME = 'session'
    public static final String ROLE_ARN = 'arn:::my-role'
    public static final int DURATION_SECONDS = 360
    public static final String EXTERNAL_ID = '123456789'

    StsClient client = Mock()

    SecurityTokenService service = new DefaultSecurityTokenService(client)

    void 'assume role'() {
        given:
            AtomicReference<AssumeRoleRequest> requestReference = new AtomicReference<>()
        when:
            // tag::usage[]
            service.assumeRole('session', 'arn:::my-role', 360) {
                externalId '123456789'
            }
            // end::usage[]
        then:
            requestReference.get()
            requestReference.get().externalId() == EXTERNAL_ID
            requestReference.get().roleSessionName() == SESSION_NAME
            requestReference.get().roleArn() == ROLE_ARN
            requestReference.get().durationSeconds() == DURATION_SECONDS

            1 * client.assumeRole(_) >> { AssumeRoleRequest request -> requestReference.set(request) }
    }

}
