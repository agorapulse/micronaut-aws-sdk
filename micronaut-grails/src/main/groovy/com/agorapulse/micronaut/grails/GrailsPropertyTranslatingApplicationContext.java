package com.agorapulse.micronaut.grails;

import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.env.Environment;

class GrailsPropertyTranslatingApplicationContext extends DefaultApplicationContext {

    private final Environment environment;

    GrailsPropertyTranslatingApplicationContext(org.springframework.core.env.Environment environment, PropertyTranslatingCustomizer customizer) {
        super(environment.getActiveProfiles());
        this.environment = new GrailsPropertyTranslatingEnvironment(environment, customizer);
    }

    @Override
    public io.micronaut.context.env.Environment getEnvironment() {
        return environment;
    }

}