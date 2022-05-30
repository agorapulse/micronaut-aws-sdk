/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema.BeanIntrospectionTableSchema;
import io.micronaut.core.beans.BeanIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;

import javax.inject.Singleton;

@Singleton
public class DefaultTableSchemaCreator implements TableSchemaCreator {

    @Override
    public <T> TableSchema<T> create(Class<T> entity) {
        if (BeanIntrospector.SHARED.findIntrospection(entity).isPresent()) {
            return BeanIntrospectionTableSchema.create(entity);
        }
        return BeanTableSchema.create(entity);
    }

}
