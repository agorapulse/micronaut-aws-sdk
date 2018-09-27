package com.agorapulse.micronaut.grails;

import java.util.*;
import java.util.regex.Pattern;

class GrailsPropertyTranslatingCustomizer implements PropertyTranslatingCustomizer, PropertyTranslatingCustomizer.Builder {

    private static class PrefixReplacement {
        final String original;
        final String replacement;

        PrefixReplacement(String original, String replacement) {
            this.original = original;
            this.replacement = replacement;
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

    private final List<PrefixReplacement> prefixPrefixReplacements = new ArrayList<>();
    private final Set<Pattern> ignored = new LinkedHashSet<>();

    private GrailsPropertyTranslatingCustomizer() {
        // prevents instantiation
    }

    /**
     * Replace property name prefixes.
     * @param original original prefix, can be empty
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
        return keys;
    }

}
