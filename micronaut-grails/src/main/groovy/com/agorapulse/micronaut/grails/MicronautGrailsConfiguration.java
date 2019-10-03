package com.agorapulse.micronaut.grails;

import io.micronaut.context.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class MicronautGrailsConfiguration {

    /**
     * Guarantees at least one importer on the classpath.
     * @return empty importer
     */
    @Bean
    MicronautBeanImporter grailsTranslatorImporter() {
        return MicronautBeanImporter.create().customize(PropertyTranslatingCustomizer.grails());
    }

    @Bean
    GrailsMicronautBeanProcessor defaultGrailsMicronautBeanProcessor(List<MicronautBeanImporter> importers) {
        Map<String, Qualifier<?>> qualifierMap = importers
            .stream()
            .flatMap(i -> i.getMicronautBeanQualifiers().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<PropertyTranslatingCustomizer> customizers = importers
            .stream()
            .flatMap(i -> i.getCustomizers().stream())
            .collect(Collectors.toList());

        return new GrailsMicronautBeanProcessor(
            qualifierMap,
            customizers
        );
    }

}
