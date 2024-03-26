/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.ses

import com.agorapulse.gru.Gru
import com.agorapulse.gru.http.Http
import com.agorapulse.micronaut.amazon.awssdk.itest.localstack.LocalstackContainerHolder
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest

import jakarta.inject.Inject
import software.amazon.awssdk.services.ses.SesClient
import spock.lang.Specification
import spock.lang.TempDir

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SES

/**
 * Tests for simple email service.
 */
@MicronautTest
@Property(name = 'aws.ses.use-base64-encoding-for-multipart-emails', value = 'true')
@Property(name = 'localstack.tag', value = '3.0.2')
class SimpleEmailServiceIntegrationSpec extends Specification {

    @TempDir File tempDir

    @Inject SimpleEmailService simpleEmailService
    @Inject SesClient sesClient
    @Inject LocalstackContainerHolder localstack
    @Inject ObjectMapper objectMapper

    void "send email with attachemnt and base64 settings"() {
        given:
            sesClient.verifyDomainIdentity { it.domain('groovycalamari.com') }

            File file = new File(tempDir, 'test.testme')
            file << 'Hello!'

        when:
            Map<String, String> customTags = [key1: 'value1', key2: 'value2']
            TransactionalEmail transactionalEmail = SimpleEmailService.email {
                subject 'Hi Paul'
                from 'subscribe@groovycalamari.com'
                to 'me@sergiodelamo.com'
                htmlBody '<p>This is an example body</p>'
                tags customTags
                attachment {
                    filename 'test.testme'
                    filepath file.absolutePath
                    mimeType 'x-application/testme'
                    description 'An example file'
                }
            }

            EmailDeliveryStatus status = simpleEmailService.send(transactionalEmail)

        then:
            status == EmailDeliveryStatus.STATUS_DELIVERED

        when:
            Gru gru = Gru.create(Http.create(this)).prepare(localstack.getEndpointOverride(SES).toString())

            gru.test {
                get('/_aws/ses')
                expect {
                    json 'emails.json'
                }
            }

        then:
            gru.verify()
    }

}
