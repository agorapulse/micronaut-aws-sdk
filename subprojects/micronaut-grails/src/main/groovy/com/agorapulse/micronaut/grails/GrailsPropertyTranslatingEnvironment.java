package com.agorapulse.micronaut.grails;

import io.micronaut.context.env.DefaultEnvironment;
import io.micronaut.core.convert.ArgumentConversionContext;
import org.springframework.core.env.Environment;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

class GrailsPropertyTranslatingEnvironment extends DefaultEnvironment {

    private final Environment environment;
    private final PropertyTranslatingCustomizer customizer;

    GrailsPropertyTranslatingEnvironment(Environment environment, PropertyTranslatingCustomizer customizer) {
        super(environment.getActiveProfiles());
        this.environment = environment;
        this.customizer = customizer;
    }

    @Override
    public io.micronaut.context.env.Environment start() {
        return this;
    }

    @Override
    public io.micronaut.context.env.Environment stop() {
        return this;
    }

    @Override
    public boolean containsProperty(@Nullable String name) {
        if (environment.containsProperty(name)) {
            return true;
        }

        Set<String> alternativeNames = customizer.getAlternativeNames(name);
        if (alternativeNames.isEmpty()) {
            return false;
        }

        return alternativeNames.stream().anyMatch(environment::containsProperty);
    }

    @Override
    public boolean containsProperties(@Nullable String name) {
        return containsProperty(name);
    }

    @Override
    public <T> Optional<T> getProperty(@Nullable String name, ArgumentConversionContext<T> conversionContext) {
        Class<T> type = conversionContext.getArgument().getType();
        T value = environment.getProperty(name, type);
        if (value != null) {
            return Optional.of(value);
        }

        Set<String> alternativeNames = customizer.getAlternativeNames(name);
        if (alternativeNames.isEmpty()) {
            return Optional.empty();
        }

        for (String alternativeName : alternativeNames) {
            T alternativeValue = environment.getProperty(alternativeName, type);
            if (alternativeValue != null) {
                return Optional.of(alternativeValue);
            }
        }

        return Optional.empty();
    }

}