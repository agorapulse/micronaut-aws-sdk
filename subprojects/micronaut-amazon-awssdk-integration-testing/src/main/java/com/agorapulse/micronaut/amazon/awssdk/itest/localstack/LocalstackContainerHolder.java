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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack;

import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.Closeable;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LocalstackContainerHolder implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalstackContainerHolder.class);
    private static final Map<LocalStackContainer.Service, GenericContainer> SHARED_CONTAINERS = new ConcurrentHashMap<>();

    private static LocalStackContainer sharedContainer;


    private final Lock startLock = new ReentrantLock();
    private final Set<LocalStackContainer.Service> enabledServices = new HashSet<>();
    private final LocalstackContainerConfiguration configuration;
    private final Map<LocalStackContainer.Service, LocalstackContainerOverridesConfiguration> overrides;
    private LocalStackContainer container;
    private final Map<LocalStackContainer.Service, GenericContainer> containers = new ConcurrentHashMap<>();

    public LocalstackContainerHolder(
        LocalstackContainerConfiguration configuration,
        List<LocalstackContainerOverridesConfiguration> configationOverrides
    ) {
        this.configuration = configuration;
        this.overrides = configationOverrides.isEmpty() ? Collections.emptyMap() : new EnumMap<LocalStackContainer.Service, LocalstackContainerOverridesConfiguration>(
            configationOverrides.stream().collect(Collectors.toMap(
                conf -> LocalStackContainer.Service.valueOf(conf.getService().toUpperCase()),
                conf -> conf
            ))
        );
        this.enabledServices.addAll(
            configuration.getServices().stream()
                .map(String::toUpperCase)
                .map(LocalStackContainer.Service::valueOf)
                .filter(s -> !this.overrides.containsKey(s))
                .collect(Collectors.toList())
        );
    }

    public LocalstackContainerHolder withServiceEnabled(LocalStackContainer.Service service) {
        enabledServices.add(service);
        return this;
    }

    public URI getEndpointOverride(LocalStackContainer.Service service) {
        return requireRunningContainer(service);
    }

    @Override
    public void close() {
        if (container != null) {
            LOGGER.info("Closing Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
            container.close();
            LOGGER.info("Closed Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        }
        containers.forEach((service, genericContainer) -> {
            LOGGER.info("Closing container {} for service {}", genericContainer.getImage(), service);
            container.close();
            LOGGER.info("Closed container {} for service {}", genericContainer.getImage(), service);
        });
    }

    public URI requireRunningContainer(LocalStackContainer.Service service) {
        LocalstackContainerOverridesConfiguration configurationOverride = overrides.get(service);
        if (configurationOverride != null) {
            if (configurationOverride.isValid()) {
                if (configurationOverride.isShared()) {
                    if (!SHARED_CONTAINERS.containsKey(service)) {
                        SHARED_CONTAINERS.put(service, createAndStartGenericContainer(configurationOverride, service));
                    }
                    GenericContainer mock = SHARED_CONTAINERS.get(service);
                    return URI.create("http://" + mock.getHost() + ":" + mock.getMappedPort(configurationOverride.getPort()));
                }

                if (!containers.containsKey(service)) {
                    containers.put(service, createAndStartGenericContainer(configurationOverride, service));
                }
                GenericContainer mock = containers.get(service);
                return URI.create("http://" + mock.getHost() + ":" + mock.getMappedPort(configurationOverride.getPort()));
            } else {
                LOGGER.warn(
                    "Configuration for overring service {} is not valid. Please, specify image, tag and port. Image: {}, Tag: {}, Port: {}",
                    configurationOverride.getService(),
                    configurationOverride.getImage(),
                    configurationOverride.getTag(),
                    configurationOverride.getPort()
                );
            }
        }

        if (configuration.isShared()) {
            if (sharedContainer == null) {
                sharedContainer = createAndStartLocalstackContainer();
            }
            return sharedContainer.getEndpointOverride(service);
        }

        if (container == null) {
            container = createAndStartLocalstackContainer();
        }

        return container.getEndpointOverride(service);
    }

    private LocalStackContainer createAndStartLocalstackContainer() {
        startLock.lock();
        LOGGER.info("Starting Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        DockerImageName dockerImageName = DockerImageName.parse(configuration.getImage()).withTag(configuration.getTag());
        LocalStackContainer container = new LocalStackContainer(dockerImageName)
            .withServices(enabledServices.toArray(new LocalStackContainer.Service[0]))
            .withEnv(configuration.getEnv());
        container.start();
        LOGGER.info("Started Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        startLock.unlock();
        return container;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private GenericContainer createAndStartGenericContainer(LocalstackContainerOverridesConfiguration configuration, LocalStackContainer.Service service) {
        startLock.lock();
        String tag = configuration.getTag();
        if (StringUtils.isEmpty(tag)) {
            tag = "latest";
        }
        LOGGER.info("Starting container {}:{} for service {}", configuration.getImage(), tag, service);
        DockerImageName dockerImageName = DockerImageName.parse(configuration.getImage()).withTag(tag);
        GenericContainer container = new GenericContainer(dockerImageName).withEnv(configuration.getEnv()).withExposedPorts(configuration.getPort());
        container.start();
        LOGGER.info("Started container {}:{} for service {}", configuration.getImage(), tag, service);
        startLock.unlock();
        return container;
    }
}
