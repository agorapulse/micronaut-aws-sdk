/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DynamoDbBeanAnnotationRemapper implements AnnotationRemapper {

    private static final String PACKAGE_NAME = "software.amazon.awssdk.enhanced.dynamodb.mapper.annotations";
    private static final String ANNOTATION_NAME = PACKAGE_NAME + ".DynamoDbBean";

    @Nonnull
    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Nonnull
    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        if (ANNOTATION_NAME.equals(annotation.getAnnotationName())) {
            return Arrays.asList(annotation, AnnotationValue.builder(Introspected.class).build());
        }
        return Collections.singletonList(annotation);
    }

}
