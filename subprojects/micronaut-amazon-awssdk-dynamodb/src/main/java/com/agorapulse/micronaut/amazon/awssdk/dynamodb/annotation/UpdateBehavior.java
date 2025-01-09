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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the behavior when this attribute is updated as part of an 'update' operation such as UpdateItem. See
 * documentation of {@link software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior} for details on the different behaviors supported and the default behavior.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface UpdateBehavior {

    Type value();

    enum Type {
        /**
         * Always overwrite with the new value if one is provided, or remove any existing value if a null value is
         * provided and 'ignoreNulls' is set to false.
         * <p>
         * This is the default behavior applied to all attributes unless otherwise specified.
         */
        WRITE_ALWAYS,

        /**
         * Write the new value if there is no existing value in the persisted record or a new record is being written,
         * otherwise leave the existing value.
         * <p>
         * IMPORTANT: If a null value is provided and 'ignoreNulls' is set to false, the attribute
         * will always be removed from the persisted record as DynamoDb does not support conditional removal with this
         * method.
         */
        WRITE_IF_NOT_EXISTS
    }
}
