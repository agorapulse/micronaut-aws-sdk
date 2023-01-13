/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.groovy;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collect the property path segments using fake property access.
 */
class PathCollector extends GroovyObjectSupport implements GroovyObject {

    public static PathCollector collectPaths(Closure<Object> collector) {
        PathCollector pathCollector = new PathCollector(null, null);
        Closure<?> closure = collector.rehydrate(pathCollector, pathCollector, pathCollector);
        closure.setResolveStrategy(Closure.DELEGATE_ONLY);
        closure.call(pathCollector);
        return pathCollector;
    }

    public PathCollector(PathCollector parent, String path) {
        this.path = path;
        this.parent = parent;
    }

    @Override
    public Object getProperty(final String propertyName) {
        return children.computeIfAbsent(propertyName, it -> new PathCollector(PathCollector.this, it));
    }

    public String getFullPath() {
        if (parent != null && parent.path != null) {
            return parent.path + "." + path;
        }

        return path;
    }

    public List<String> getPropertyPaths() {
        if (!children.isEmpty()) {
            final List<String> result = new ArrayList<String>();
            children.values().forEach(it -> result.addAll(it.getPropertyPaths()));
            return result;
        }

        return Collections.singletonList(getFullPath());
    }

    private final String path;
    private final PathCollector parent;
    private final Map<String, PathCollector> children = new LinkedHashMap<String, PathCollector>();
}
