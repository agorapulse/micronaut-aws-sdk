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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.remap;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import io.micronaut.core.annotation.NonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DynamoDbBeanAnnotationRemapper implements AnnotationRemapper {

    private static final String PACKAGE_NAME = "software.amazon.awssdk.enhanced.dynamodb.mapper.annotations";
    private static final String DYNAMODB_BEAN = PACKAGE_NAME + ".DynamoDbBean";
    private static final String DYNAMODB_IMMUTABLE = PACKAGE_NAME + ".DynamoDbImmutable";
    private static final String BUILDER_ATTRIBUTE = "builder";

    @NonNull
    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @NonNull
    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        String annotationName = annotation.getAnnotationName();

        if (DYNAMODB_BEAN.equals(annotationName)) {
            return Arrays.asList(annotation, AnnotationValue.builder(Introspected.class).build());
        }

        if (DYNAMODB_IMMUTABLE.equals(annotationName)) {
            AnnotationValue<Introspected> introspectedAnnotation;

            // Map the builder class to Introspected.IntrospectionBuilder
            Optional<Class<?>> builderClassOptional = annotation.classValue(BUILDER_ATTRIBUTE);
            if (builderClassOptional.isPresent()) {
                Class<?> builderClass = builderClassOptional.get();
                AnnotationValue<Introspected.IntrospectionBuilder> builderAnnotation =
                    AnnotationValue.builder(Introspected.IntrospectionBuilder.class)
                        .member("builderClass", builderClass)
                        .build();
                introspectedAnnotation = AnnotationValue.builder(Introspected.class)
                    .member(BUILDER_ATTRIBUTE, builderAnnotation)
                    .build();
                return List.of(
                    annotation,
                    introspectedAnnotation,
                    AnnotationValue.builder("com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Immutable").build()
                );
            }
            return List.of(annotation, AnnotationValue.builder(Introspected.class).build());
        }

        return Collections.singletonList(annotation);
    }

}
