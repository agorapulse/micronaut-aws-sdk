package com.agorapulse.micronaut.http.examples;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;

/**
 * Entry point to the example application.
 */
public class Application {

    private static ApplicationContext context;

    public static ApplicationContext getContext() {
        return context;
    }

    public static void main(String[] args) {
        context = Micronaut.run(Application.class);
    }

}
