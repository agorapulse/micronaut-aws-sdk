package com.agorapulse.micronaut.http.examples

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.Micronaut

/**
 * Entry point to the example application.
 */
class Application {

    static ApplicationContext context

    static void main(String[] args) {
        context = Micronaut.run(Application)
    }

}
