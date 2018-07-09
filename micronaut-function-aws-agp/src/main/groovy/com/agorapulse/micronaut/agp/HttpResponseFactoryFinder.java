package com.agorapulse.micronaut.agp;

import com.agorapulse.micronaut.http.basic.BasicHttpResponseFactory;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponseFactory;

import javax.inject.Singleton;

@Factory
public class HttpResponseFactoryFinder {

    @Bean
    @Singleton
    public HttpResponseFactory httpResponseFactory(Environment environment) {
        if (environment.getActiveNames().contains(ApiGatewayProxyHandler.API_GATEWAY_PROXY_ENVIRONMENT)) {
            return new BasicHttpResponseFactory();
        }

        return HttpResponseFactory.INSTANCE.orElseThrow(() ->
            new IllegalStateException("No Server implementation found on classpath")
        );
    }
}
