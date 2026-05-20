/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.ministack;

import io.micronaut.core.util.StringUtils;
import org.ministack.testcontainers.MiniStackContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.Closeable;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class MinistackContainerHolder implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinistackContainerHolder.class);
    private static final Map<String, GenericContainer<?>> SHARED_CONTAINERS = new ConcurrentHashMap<>();

    private static MiniStackContainer sharedContainer;

    private final Lock startLock = new ReentrantLock();
    private final MinistackContainerConfiguration configuration;
    private final Map<String, MinistackContainerOverridesConfiguration> overrides;
    private final Map<String, GenericContainer<?>> containers = new ConcurrentHashMap<>();
    private MiniStackContainer container;

    public MinistackContainerHolder(
        MinistackContainerConfiguration configuration,
        List<MinistackContainerOverridesConfiguration> configurationOverrides
    ) {
        this.configuration = configuration;
        this.overrides = configurationOverrides.isEmpty()
            ? Collections.emptyMap()
            : configurationOverrides.stream().collect(Collectors.toMap(
                conf -> conf.getService().toUpperCase(Locale.ROOT),
                conf -> conf
            ));
    }

    public URI getEndpointOverride(String service) {
        return requireRunningContainer(service);
    }

    @Override
    public void close() {
        if (container != null) {
            LOGGER.info("Closing MiniStack container with tag {}", configuration.getTag());
            container.close();
            LOGGER.info("Closed MiniStack container with tag {}", configuration.getTag());
        }
        containers.forEach((service, genericContainer) -> {
            LOGGER.info("Closing container {} for service {}", genericContainer.getDockerImageName(), service);
            genericContainer.close();
            LOGGER.info("Closed container {} for service {}", genericContainer.getDockerImageName(), service);
        });
    }

    public URI requireRunningContainer(String service) {
        String serviceKey = service.toUpperCase(Locale.ROOT);
        MinistackContainerOverridesConfiguration configurationOverride = overrides.get(serviceKey);
        if (configurationOverride != null) {
            if (configurationOverride.isValid()) {
                if (configurationOverride.isShared()) {
                    GenericContainer<?> shared = SHARED_CONTAINERS.computeIfAbsent(
                        serviceKey,
                        s -> createAndStartGenericContainer(configurationOverride, s)
                    );
                    return URI.create("http://" + shared.getHost() + ":" + shared.getMappedPort(configurationOverride.getPort()));
                }

                GenericContainer<?> mock = containers.computeIfAbsent(
                    serviceKey,
                    s -> createAndStartGenericContainer(configurationOverride, s)
                );
                return URI.create("http://" + mock.getHost() + ":" + mock.getMappedPort(configurationOverride.getPort()));
            } else {
                LOGGER.warn(
                    "Configuration for overriding service {} is not valid. Please, specify image, tag and port. Image: {}, Tag: {}, Port: {}",
                    configurationOverride.getService(),
                    configurationOverride.getImage(),
                    configurationOverride.getTag(),
                    configurationOverride.getPort()
                );
            }
        }

        if (configuration.isShared()) {
            if (sharedContainer == null) {
                sharedContainer = createAndStartMinistackContainer();
            }
            return URI.create(sharedContainer.getEndpoint());
        }

        if (container == null) {
            container = createAndStartMinistackContainer();
        }

        return URI.create(container.getEndpoint());
    }

    private MiniStackContainer createAndStartMinistackContainer() {
        startLock.lock();
        try {
            String tag = configuration.getTag();
            LOGGER.info("Starting MiniStack container with tag {}", tag);
            MiniStackContainer ministack = StringUtils.isNotEmpty(tag)
                ? new MiniStackContainer(tag)
                : new MiniStackContainer();

            Map<String, String> env = new HashMap<>(configuration.getEnv());
            if (StringUtils.isNotEmpty(configuration.getRegion())) {
                env.putIfAbsent("MINISTACK_REGION", configuration.getRegion());
            }
            if (StringUtils.isNotEmpty(configuration.getAccessKey())) {
                env.putIfAbsent("AWS_ACCESS_KEY_ID", configuration.getAccessKey());
            }
            if (StringUtils.isNotEmpty(configuration.getSecretKey())) {
                env.putIfAbsent("AWS_SECRET_ACCESS_KEY", configuration.getSecretKey());
            }
            if (configuration.isPersistence()) {
                env.putIfAbsent("PERSIST_STATE", "1");
                env.putIfAbsent("S3_PERSIST", "1");
            }
            if (configuration.isImdsV2Required()) {
                env.putIfAbsent("MINISTACK_IMDS_V2_REQUIRED", "1");
            }
            if (!env.isEmpty()) {
                ministack.withEnv(env);
            }
            if (configuration.isRealInfrastructure()) {
                ministack.withRealInfrastructure();
            }

            ministack.start();
            LOGGER.info("Started MiniStack container with tag {}", tag);
            return ministack;
        } finally {
            startLock.unlock();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private GenericContainer createAndStartGenericContainer(MinistackContainerOverridesConfiguration configuration, String service) {
        startLock.lock();
        try {
            String tag = configuration.getTag();
            if (StringUtils.isEmpty(tag)) {
                tag = "latest";
            }
            LOGGER.info("Starting container {}:{} for service {}", configuration.getImage(), tag, service);
            DockerImageName dockerImageName = DockerImageName.parse(configuration.getImage()).withTag(tag);
            GenericContainer container = new GenericContainer(dockerImageName)
                .withEnv(configuration.getEnv())
                .withExposedPorts(configuration.getPort());
            container.start();
            LOGGER.info("Started container {}:{} for service {}", configuration.getImage(), tag, service);
            return container;
        } finally {
            startLock.unlock();
        }
    }

}
