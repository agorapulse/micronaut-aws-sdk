package com.agorapulse.micronaut.amazon.awssdk.localstack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

import java.io.Closeable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LocalstackContainerHolder implements AwsRegionProvider, AwsCredentialsProvider, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalstackContainerHolder.class);

    private final Lock startLock = new ReentrantLock();
    private final List<LocalStackContainer.Service> enabledServices = new ArrayList<>();
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

    @Override
    public Region getRegion() {
        return Region.of(ensureStarted().getRegion());
    }

    @Override
    public AwsCredentials resolveCredentials() {
        LocalStackContainer localstack = ensureStarted();
        return AwsBasicCredentials.create(
            localstack.getAccessKey(), localstack.getSecretKey()
        );
    }

    public URI getEndpointOverride(LocalStackContainer.Service service) {
        return ensureStarted().getEndpointOverride(service);
    }

    @Override
    public void close() {
        container.close();
    }

    private LocalStackContainer ensureStarted() {
        startLock.lock();
        if (container == null) {
            LOGGER.info("Starting Localstack container {}:{} for services {}", configuration.getImage(), configuration.getTag(), enabledServices);
            DockerImageName dockerImageName = DockerImageName.parse(configuration.getImage()).withTag(configuration.getTag());
            container = new LocalStackContainer(dockerImageName).withServices(enabledServices.toArray(new LocalStackContainer.Service[0]));
            container.start();
            LOGGER.info("Container started");
        }
        startLock.unlock();
        return container;
    }
}
