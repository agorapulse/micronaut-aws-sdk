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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.ApplicationConfiguration;

import javax.inject.Named;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

/**
 * Default configuration for Kinesis listener.
 */
@Named("default")
@ConfigurationProperties("aws.kinesis.listener")
public class DefaultKinesisClientConfiguration extends KinesisClientConfiguration {

    public DefaultKinesisClientConfiguration(
        @Value("${aws.kinesis.application.name}") Optional<String> applicationName,
        @Value("${aws.kinesis.worker.id}") Optional<String> workerId,
        ApplicationConfiguration applicationConfiguration
    ) {
        super(
            applicationName.orElseGet(() -> applicationConfiguration.getName().orElseThrow(() ->
                new IllegalStateException("Missing application name. Please, set either micronaut.application.name or aws.kinesis.application.name")
            )),
            workerId.orElseGet(() -> {
                try {
                    return InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
                } catch (UnknownHostException e) {
                    return "unknown:" + UUID.randomUUID();
                }
            })
        );
    }

}
