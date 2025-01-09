/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.FunctionEvaluator;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.JavaFunctionEvaluator;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.StrictMap;
import groovy.lang.Closure;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;

import jakarta.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;

@Singleton
@Requires(classes = Closure.class)
public class GroovyFunctionEvaluator implements FunctionEvaluator {

    private final FunctionEvaluator delegate = new JavaFunctionEvaluator();

    public <T, F extends Function<Map<String, Object>, T>> T evaluateAnnotationType(Class<F> updateDefinitionType, MethodInvocationContext<Object, Object> context) {
        Map<String, Object> parameterValueMap = new StrictMap<>(context.getParameterValueMap());

        if (Closure.class.isAssignableFrom(updateDefinitionType)) {
            try {
                Closure<T> closure = (Closure<T>) updateDefinitionType.getConstructor(Object.class, Object.class).newInstance(parameterValueMap, parameterValueMap);
                closure.setDelegate(parameterValueMap);
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                return closure.call(parameterValueMap);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot instantiate closure! Type: " + updateDefinitionType, e);
            }
        }

        return delegate.evaluateAnnotationType(updateDefinitionType, context);
    }

}
