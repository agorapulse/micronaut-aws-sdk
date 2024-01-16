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

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;

class EntityIntrospection {

    static <T> BeanIntrospection<T> getBeanIntrospection(MappedTableResource<T> table) {
        return BeanIntrospector.SHARED.findIntrospection(table.tableSchema().itemType().rawClass())
            .orElseThrow(() -> new IllegalArgumentException("No introspection found for " + table.tableSchema().itemType().rawClass()
                + "! Please, annotate the class with @Introspected. See https://docs.micronaut.io/latest/guide/index.html#introspection for more details")
            );
    }

}
