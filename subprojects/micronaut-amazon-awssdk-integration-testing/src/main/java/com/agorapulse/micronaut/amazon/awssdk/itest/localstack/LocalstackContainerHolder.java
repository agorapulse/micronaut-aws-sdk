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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.Closeable;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LocalstackContainerHolder implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalstackContainerHolder.class);

    private static LocalStackContainer sharedContainer;

    private final Lock startLock = new ReentrantLock();
    private final Set<LocalStackContainer.Service> enabledServices = new HashSet<>();
    private final LocalstackContainerConfiguration configuration;
    private LocalStackContainer container;

    public LocalstackContainerHolder(LocalstackContainerConfiguration configuration) {
        this.configuration = configuration;
        this.enabledServices.addAll(
            configuration.getServices().stream()
                .map(String::toUpperCase)
                .map(LocalStackContainer.Service::valueOf)
                .collect(Collectors.toList())
        );
    }

    public LocalstackContainerHolder withServiceEnabled(LocalStackContainer.Service service) {
        enabledServices.add(service);
        return this;
    }

    public URI getEndpointOverride(LocalStackContainer.Service service) {
        return requireRunningContainer().getEndpointOverride(service);
    }

    @Override
    public void close() {
        if (container != null) {
            LOGGER.info("Closing Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
            container.close();
            LOGGER.info("Closed Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        }
    }

    public LocalStackContainer requireRunningContainer() {
        if (configuration.isShared()) {
            if (sharedContainer == null) {
                sharedContainer = createAndStartContainer();
            }
            return sharedContainer;
        }

        if (container == null) {
            container = createAndStartContainer();
        }

        return container;
    }

    private LocalStackContainer createAndStartContainer() {
        startLock.lock();
        LOGGER.info("Starting Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        DockerImageName dockerImageName = DockerImageName.parse(configuration.getImage()).withTag(configuration.getTag());
        LocalStackContainer container = new LocalStackContainer(dockerImageName).withServices(enabledServices.toArray(new LocalStackContainer.Service[0]));
        container.start();
        LOGGER.info("Started Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
        startLock.unlock();
        return container;
    }
}
