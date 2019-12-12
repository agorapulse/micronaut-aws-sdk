/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.grails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

    static PropertyTranslatingCustomizer of(final Collection<PropertyTranslatingCustomizer> customizers) {
        return name -> customizers.stream().flatMap(c -> c.getAlternativeNames(name).stream()).collect(Collectors.toSet());
    }

    /**
     * @param name original property name
     * @return set of alternative names for the original property name
     */
    Set<String> getAlternativeNames(String name);

}
