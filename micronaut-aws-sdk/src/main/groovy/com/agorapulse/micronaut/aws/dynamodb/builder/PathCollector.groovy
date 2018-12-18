package com.agorapulse.micronaut.aws.dynamodb.builder

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
class PathCollector {

    static PathCollector collectPaths(Closure<Object> collector) {
        PathCollector pathCollector = new PathCollector(null, null)
        Closure closure = collector.rehydrate(pathCollector, pathCollector, pathCollector);
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call(pathCollector)
        pathCollector
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
        return children.computeIfAbsent(propertyName) { new PathCollector(this, propertyName)}
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
