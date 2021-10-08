package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.ApplicationConfiguration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

/**
 * Kinesis listener configuration for given configuration key.
 */
@EachProperty("aws.kinesis.listeners")
public class NamedKinesisClientConfiguration extends KinesisClientConfiguration {
    private final String name;

    public NamedKinesisClientConfiguration(
        @Parameter String name,
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
        this.name = name;
    }

    public final String getName() {
        return name;
    }

}
