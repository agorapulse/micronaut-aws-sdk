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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBEntity
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBEntityNoRange
import io.micronaut.context.BeanContext
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import spock.lang.Specification

class BeanIntrospectionTableSchemaSpec extends Specification {

    MetaTableSchemaCache cache = new MetaTableSchemaCache()

    BeanContext context = Mock {
        findBean(_) >> Optional.empty()
    }

    void 'read table schema for groovy class'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntityNoRange> schema = BeanIntrospectionTableSchema.create(DynamoDBEntityNoRange, context, cache)
        then:
            schema.attributeNames().size() == 3
    }

    void 'verify converters'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntity> schema = BeanIntrospectionTableSchema.create(DynamoDBEntity, context, cache)
        then:
            schema.itemToMap(new DynamoDBEntity(date: new Date()), ['date']).get('date').s()
    }

}
