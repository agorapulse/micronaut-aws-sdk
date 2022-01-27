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
package com.agorapulse.micronaut.aws.apigateway.ws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link MessageSenderFactory}.
 */
@Singleton
public class DefaultMessageSenderFactory implements MessageSenderFactory {

    private final AWSCredentialsProvider credentialsProvider;
    private final AwsRegionProvider regionProvider;
    private final ObjectMapper mapper;
    private final Map<String, MessageSender> senders = new ConcurrentHashMap<>();
    private final Optional<String> region;

    public DefaultMessageSenderFactory(
        AWSCredentialsProvider credentialsProvider,
        AwsRegionProvider regionProvider,
        ObjectMapper mapper,
        @Value("${aws.websocket.region}") Optional<String> region
    ) {
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.mapper = mapper;
        this.region = region;
    }

    @Override
    public MessageSender create(String connectionsUrl) {
        return senders.computeIfAbsent(connectionsUrl, url -> new DefaultMessageSender(url, credentialsProvider, region.orElseGet(regionProvider::getRegion), mapper));
    }
}
