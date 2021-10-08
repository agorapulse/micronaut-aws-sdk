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
package com.agorapulse.micronaut.amazon.awssdk.kinesis;


import io.micronaut.context.ApplicationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;

import static org.junit.Assert.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

// tag::testcontainers-spec[]
public class KinesisJavaDemoTest {

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()                   // <1>
        .withServices(KINESIS);

    public ApplicationContext context;
    public KinesisService service;

    @Before
    public void setup() {
        // disable CBOR (not supported by Kinelite)
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), "false");          // <2>

        KinesisClient kinesis = KinesisClient                                           // <3>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey()
                    )
                )
            )
            .region(Region.EU_WEST_1)
            .build();

        context = ApplicationContext.builder().build();                                 // <4>
        context.registerSingleton(KinesisClient.class, kinesis);
        context.start();

        service = context.getBean(KinesisService.class);                                // <5>
    }

    @After
    public void cleanup() {
        if (context != null) {
            context.close();                                                            // <6>
        }
    }

    @Test
    public void testJavaService() {
        assertNotNull(service.createStream("TestStream"));
    }
}
// end::testcontainers-spec[]
