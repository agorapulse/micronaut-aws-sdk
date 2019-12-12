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
