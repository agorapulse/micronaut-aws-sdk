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

import io.micronaut.context.env.MapPropertySource;
import io.micronaut.context.env.SystemPropertiesPropertySource;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Property source which translates system properties which looks like environment variables e.g. REDIS_PORT to
 * properties.
 */
public class EnvVarLikeSystemPropertiesPropertySource extends MapPropertySource {

    public static final int POSITION = SystemPropertiesPropertySource.POSITION - 50;

    /**
     * Constant for System property source.
     */
    public static final String NAME = "envAsSystem";

    /**
     * Default constructor.
     */
    public EnvVarLikeSystemPropertiesPropertySource() {
        super(NAME, getUpperCasedProperties());
    }

    private static Map<Object, Object> getUpperCasedProperties() {
        return System.getProperties()
            .entrySet()
            .stream()
            // the value upper case is same as the value means that every character is uppercase or punctation
            .filter(e -> String.valueOf(e.getKey()).toUpperCase().equals(String.valueOf(e.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public int getOrder() {
        return POSITION;
    }

    @Override
    public PropertyConvention getConvention() {
        return PropertyConvention.ENVIRONMENT_VARIABLE;
    }

}
