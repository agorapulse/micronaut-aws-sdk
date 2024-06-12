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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import jakarta.inject.Singleton;

/**
 * Introduction for {@link com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service} annotation.
 */
@Singleton
public class ServiceIntroduction implements MethodInterceptor<Object, Object> {

    private final DynamoDbServiceIntroduction introduction;

    public ServiceIntroduction(DynamoDbServiceIntroduction introduction) {
        this.introduction = introduction;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        AnnotationValue<Service> serviceAnnotationValue = context.getAnnotation(Service.class);

        if (serviceAnnotationValue == null) {
            throw new IllegalStateException("Invocation context is missing required annotation Service");
        }

        return doIntercept(context, serviceAnnotationValue);
    }

    @SuppressWarnings("unchecked")
    private <T> Object doIntercept(MethodInvocationContext<Object, Object> context, AnnotationValue<Service> serviceAnnotationValue) {
        Class<T> type = (Class<T>) serviceAnnotationValue.classValue().orElseThrow(() -> new IllegalArgumentException("Annotation is missing the type value!"));
        String tableName = serviceAnnotationValue.stringValue("tableName").orElseGet(type::getSimpleName);

        return introduction.doIntercept(context, type, tableName);
    }

}
