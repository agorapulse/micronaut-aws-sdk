package com.agorapulse.micronaut.aws.apigateway.ws;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;

/**
 * Factory to create default message sender for configuration value.
 */
@Factory
public class WebSocketComponentsFactory {

    @Bean
    @Singleton
    @Requires(property = "aws.websocket.connections.url")
    MessageSender defaultMessageSender(@Value("${aws.websocket.connections.url}") String connectionsUrl, MessageSenderFactory factory) {
        return factory.create(connectionsUrl);
    }
}
