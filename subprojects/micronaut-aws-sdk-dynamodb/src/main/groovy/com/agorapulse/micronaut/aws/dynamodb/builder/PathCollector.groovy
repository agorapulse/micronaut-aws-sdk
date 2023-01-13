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
package com.agorapulse.micronaut.aws.dynamodb.builder

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 * Collect the property path segments using fake property access.
 */
@CompileStatic
@PackageScope
class PathCollector {

    static PathCollector collectPaths(Closure<Object> collector) {
        PathCollector pathCollector = new PathCollector(null, null)
        Closure closure = collector.rehydrate(pathCollector, pathCollector, pathCollector)
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call(pathCollector)
        return pathCollector
    }

    private final String path
    private final PathCollector parent
    private final Map<String, PathCollector> children = [:]

    PathCollector(PathCollector parent, String path) {
        this.path = path
        this.parent = parent
    }

    @Override
    Object getProperty(String propertyName) {
        return children.computeIfAbsent(propertyName) { new PathCollector(this, propertyName) }
    }

    String getFullPath() {
        if (parent && parent.path) {
            return "${parent.path}.${path}"
        }
        return path
    }

    List<String> getPropertyPaths() {
        if (children) {
            List<String> result = []
            children.values().each {
                result.addAll(it.propertyPaths)
            }
            return result
        }

        return Collections.singletonList(fullPath)
    }

}
