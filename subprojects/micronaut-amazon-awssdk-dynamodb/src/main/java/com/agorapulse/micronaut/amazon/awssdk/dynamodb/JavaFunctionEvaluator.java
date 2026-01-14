/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.util.StrictMap;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.function.Function;

@Singleton
@Requires(missingClasses = "groovy.lang.Closure")
public class JavaFunctionEvaluator implements FunctionEvaluator {

    @Override
    public <T, F extends Function<Map<String, Object>, T>> T evaluateAnnotationType(Class<F> updateDefinitionType, MethodInvocationContext<Object, Object> context) {
        Map<String, Object> parameterValueMap = new StrictMap<>(context.getParameterValueMap());
        F function = BeanIntrospector.SHARED.findIntrospection(updateDefinitionType).map(BeanIntrospection::instantiate)
            .orElseGet(() -> {
                try {
                    return updateDefinitionType.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Cannot instantiate function! Type: " + updateDefinitionType, e);
                }
            });
        return function.apply(parameterValueMap);
    }
}
