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

import java.lang.annotation.*;

/**
 * Annotates partition key.
 *
 * This annotation is not required if the name of the argument contains word <code>hash</code> or <code>partition</code>.
 *
 * This annotation can be used a replacement of <code>{@link software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey}</code>
 * on fields.
 */
// TODO: annotation mapper
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface PartitionKey { }
