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
package com.agorapulse.micronaut.aws.s3

import io.micronaut.context.annotation.Property
import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

import java.time.Duration

/**
 * Tests for SimpleStorageService based on Testcontainers with Minio server.
 */
@Property(name = 'aws.s3.path-style-access-enabled', value = 'true')
class SimpleStorageServiceMinioSpec extends SimpleStorageServiceSpec implements TestPropertyProvider {

    private static final int MINIO_PORT = 9000

    @Override
    Map<String, String> getProperties() {
        GenericContainer minioContainer = new GenericContainer<>('minio/minio:latest')
            .withCommand('server /data')
            .withEnv([
                MINIO_ACCESS_KEY:'accesskey',
                MINIO_SECRET_KEY:'secretkey'
            ])
            .withExposedPorts(MINIO_PORT)
            .waitingFor(new HttpWaitStrategy()
                .forPath('/minio/health/ready')
                .forPort(MINIO_PORT)
                .withStartupTimeout(Duration.ofSeconds(10)))
        minioContainer.start()
        return [
            'aws.s3.endpoint':"http://$minioContainer.host:$minioContainer.firstMappedPort",
        ]
    }

}
