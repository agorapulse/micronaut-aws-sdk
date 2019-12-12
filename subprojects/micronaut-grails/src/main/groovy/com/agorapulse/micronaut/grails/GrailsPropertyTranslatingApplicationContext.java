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

import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.env.Environment;

class GrailsPropertyTranslatingApplicationContext extends DefaultApplicationContext {

    private final Environment environment;

    GrailsPropertyTranslatingApplicationContext(org.springframework.core.env.Environment environment, PropertyTranslatingCustomizer customizer) {
        super(environment.getActiveProfiles());
        this.environment = new GrailsPropertyTranslatingEnvironment(environment, customizer);
        this.environment.addPropertySource(new EnvVarLikeSystemPropertiesPropertySource());
    }

    @Override
    public io.micronaut.context.env.Environment getEnvironment() {
        return environment;
    }

}
