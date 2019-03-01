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
