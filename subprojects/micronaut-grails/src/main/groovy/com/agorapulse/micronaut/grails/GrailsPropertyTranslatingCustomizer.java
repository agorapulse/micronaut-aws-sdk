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

import io.micronaut.core.naming.NameUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class GrailsPropertyTranslatingCustomizer implements PropertyTranslatingCustomizer, PropertyTranslatingCustomizer.Builder {

    private static class PrefixReplacement {
        final String original;
        final String replacement;

        PrefixReplacement(String original, String replacement) {
            this.original = original;
            this.replacement = replacement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PrefixReplacement that = (PrefixReplacement) o;
            return Objects.equals(original, that.original) && Objects.equals(replacement, that.replacement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(original, replacement);
        }

        @Override
        public String toString() {
            return original + " -> " + replacement;
        }
    }

    private static final String GRAILS_PREFIX = "grails.";
    private static final String MICRONAUT_PREFIX = "micronaut.";
    private static final String EMPTY_STRING = "";

    static GrailsPropertyTranslatingCustomizer create() {
        return new GrailsPropertyTranslatingCustomizer();
    }

    static GrailsPropertyTranslatingCustomizer standard() {
        return new GrailsPropertyTranslatingCustomizer()
            .replacePrefix(MICRONAUT_PREFIX, GRAILS_PREFIX)
            .replacePrefix(EMPTY_STRING, GRAILS_PREFIX);
    }

    static List<PropertyTranslatingCustomizer> collapse(List<PropertyTranslatingCustomizer> customizers) {
        List<PropertyTranslatingCustomizer> result = new ArrayList<>();
        GrailsPropertyTranslatingCustomizer customizer = create();
        result.add(customizer);

        for (PropertyTranslatingCustomizer c : customizers) {
            if (c instanceof GrailsPropertyTranslatingCustomizer) {
                GrailsPropertyTranslatingCustomizer gc = (GrailsPropertyTranslatingCustomizer) c;
                customizer.prefixPrefixReplacements.addAll(gc.prefixPrefixReplacements);
                customizer.ignored.addAll(gc.ignored);
            } else {
                result.add(c);
            }
        }

        return result;
    }

    private final Set<PrefixReplacement> prefixPrefixReplacements = new LinkedHashSet<>();
    private final Set<Pattern> ignored = new LinkedHashSet<>();

    private GrailsPropertyTranslatingCustomizer() {
        // prevents instantiation
    }

    /**
     * Replace property name prefixes.
     *
     * @param original    original prefix, can be empty
     * @param replacement new prefix, can be empty
     * @return self
     */
    @Override
    public GrailsPropertyTranslatingCustomizer replacePrefix(String original, String replacement) {
        prefixPrefixReplacements.add(new PrefixReplacement(original, replacement));
        return this;
    }

    /**
     * Ignores exact match of the property name.
     *
     * @param property the property to be ignored
     * @return self
     */
    @Override
    public GrailsPropertyTranslatingCustomizer ignore(String property) {
        ignored.add(Pattern.compile(Pattern.quote(property)));
        return this;
    }

    /**
     * Ignores by regex match of the property pattern.
     *
     * @param propertyPattern the pattern of properties being ignored
     * @return self
     */
    @Override
    public GrailsPropertyTranslatingCustomizer ignoreAll(String propertyPattern) {
        ignored.add(Pattern.compile(propertyPattern));
        return this;
    }

    @Override
    public PropertyTranslatingCustomizer build() {
        return this;
    }

    @Override
    public Set<String> getAlternativeNames(String name) {
        if (name == null || name.length() == 0) {
            return Collections.emptySet();
        }

        if (ignored.stream().anyMatch(p -> p.matcher(name).matches())) {
            return Collections.emptySet();
        }

        Set<String> keys = new LinkedHashSet<>(prefixPrefixReplacements.size());
        prefixPrefixReplacements.forEach(replacement -> {
            if ((replacement.original.isEmpty() || name.startsWith(replacement.original)) && (replacement.replacement.isEmpty() || !name.startsWith(replacement.replacement))) {
                String keyNoPrefix = name.substring(replacement.original.length());
                String alternativeKey = replacement.replacement + keyNoPrefix;
                if (!alternativeKey.equals(name)) {
                    keys.add(alternativeKey);
                }
            }
        });

        keys.addAll(keys.stream().map(GrailsPropertyTranslatingCustomizer::dehyphenate).collect(Collectors.toSet()));

        return keys;
    }

    private static String dehyphenate(String key) {
        return Arrays.stream(key.split("\\."))
            .map(NameUtils::dehyphenate)
            .map(name -> {
                if (name.length() <= 1) {
                    return name.toLowerCase();
                }
                return name.substring(0, 1).toLowerCase() + name.substring(1);
            })
            .collect(Collectors.joining("."));
    }

}
