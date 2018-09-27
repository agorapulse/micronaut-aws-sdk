package com.agorapulse.micronaut.grails;

import java.util.Collections;
import java.util.Set;

/**
 * Property translating customizer add an ability to re-use existing properties defined in Spring application context
 * in Micronaut context using simple translating rules.
 */
public interface PropertyTranslatingCustomizer {

    interface Builder {
        /**
         * Replace property name prefixes.
         * @param original original prefix, can be empty
         * @param replacement new prefix, can be empty
         * @return self
         */
        Builder replacePrefix(String original, String replacement);

        /**
         * Ignores exact match of the property name.
         * @param property the property to be ignored
         * @return self
         */
        Builder ignore(String property);

        /**
         * Ignores by regex match of the property pattern.
         * @param propertyPattern the pattern of properties being ignored
         * @return self
         */
        Builder ignoreAll(String propertyPattern);

        /**
         * @return the customizer instance
         */
        PropertyTranslatingCustomizer build();
    }

    static PropertyTranslatingCustomizer none() {
        return name -> Collections.emptySet();
    }

    /**
     * Create new customizer builder.
     * @return self
     */
    static PropertyTranslatingCustomizer.Builder builder() {
        return GrailsPropertyTranslatingCustomizer.create();
    }

    /**
     * @return builder for grails customizer which replaces Micronaut prefix with Grails and also allows to skip the Grails prefix
     */
    static PropertyTranslatingCustomizer.Builder grails() {
        return GrailsPropertyTranslatingCustomizer.standard();
    }

    /**
     * @param name original property name
     * @return set of alternative names for the original property name
     */
    Set<String> getAlternativeNames(String name);

}
