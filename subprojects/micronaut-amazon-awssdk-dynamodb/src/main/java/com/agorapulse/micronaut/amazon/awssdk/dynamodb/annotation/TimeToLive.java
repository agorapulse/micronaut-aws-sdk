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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation;

import java.lang.annotation.*;


/**
 * Specifies the time to live for entity.
 * <p>
 * When a class is annotated with then a new attribute specified by {@link #attributeName()} is added to the entity every time
 * the entity is persisted. The value of the attribute is set to the current time plus the duration specified by {@link #value()}.
 * The value of the attribute is updated every time the entity is persisted.
 * </p>
 * <p>
 * When a field is annotated with the annotation, the field value is used to determine the time to live for the entity.
 * The value is computed by adding the value of the field plus the duration specified by {@link #value()}. The value of the field
 * is updated every time the entity is persisted.
 * </p>
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface TimeToLive {

    /**
     * The duration string that represents the time to live for the item.
     *
     * @return the duration string that represents the time to live for the item
     */
    String value();

    /**
     * The name of the attribute that represents the time to live for the item.
     */
    String attributeName() default "ttl";

    /**
     * The format of the time if the annotated member is a field of type {@link String}.
     * By default, the time will be parsed using {@link java.time.Instant#parse(CharSequence)}.
     * The value has no effect if the annotated member is not a field of type {@link String} or the annotation is used on a type.
     *
     * @return the format of the time if the annotated member is a field of type {@link String}
     */
    String format() default "";

}
