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
package com.agorapulse.micronaut.amazon.awssdk.kinesis

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkSystemSetting
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse
import spock.lang.AutoCleanup
import spock.lang.Retry
import spock.lang.Shared
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS

/**
 * Tests for kinesis service.
 */
// tag::testcontainers-spec[]
@Testcontainers                                                                         // <1>
@RestoreSystemProperties                                                                // <2>
class KinesisDemoSpec extends Specification {

    @Shared
    LocalStackContainer localstack = new LocalStackContainer().withServices(KINESIS)    // <3>

    @AutoCleanup ApplicationContext context                                             // <4>

    KinesisService service

    void setup() {
        // disable CBOR (not supported by Kinelite)
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), 'false')           // <5>

        KinesisClient kinesis = KinesisClient                                           // <6>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.accessKey, localstack.secretKey
                    )
                )
            )
            .region(Region.EU_WEST_1)
            .build()

        context = ApplicationContext.builder().build()                                  // <7>
        context.registerSingleton(KinesisClient, kinesis)
        context.start()

        service = context.getBean(KinesisService)                                       // <8>
    }

    @Retry
    void 'new default stream'() {
        when:
            CreateStreamResponse stream = service.createStream('NewStream')
        then:
            stream
    }

}
// end::testcontainers-spec[]
