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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Address
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBEntity
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBEntityMapProperty
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBEntityNoRange
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Person
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.PhoneNumber
import io.micronaut.context.BeanContext
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import spock.lang.IgnoreIf
import spock.lang.Specification

class BeanIntrospectionTableSchemaSpec extends Specification {

    MetaTableSchemaCache cache = new MetaTableSchemaCache()

    BeanContext context = Mock {
        findBean(_) >> Optional.empty()
    }

    void 'read table schema for java class'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntity> schema = BeanIntrospectionTableSchema.create(DynamoDBEntity, context, cache)
        then:
            schema.attributeNames().size() == 9
    }

    void 'read table schema for java class with nested beans'() {
        when:
            BeanIntrospectionTableSchema<Person> schema = BeanIntrospectionTableSchema.create(Person, context, cache)
        then:
            schema.attributeNames().size() == 8
        when:
            schema.itemToMap(new Person(
                id: 1,
                firstName: 'John',
                lastName: 'Doe',
                age: 42,
                addresses: [
                    main: new Address(
                        street: 'Main Street',
                        city: 'Springfield',
                        zipCode: '12345'
                    )
                ],
                phoneNumbers: [new PhoneNumber(type: 'home', number: '123456789')],
                hobbies: ['reading', 'coding'],
                favoriteColors: ['red', 'green'],
            ), true)
        then:
            noExceptionThrown()
    }

    void 'read table schema for groovy class'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntityNoRange> schema = BeanIntrospectionTableSchema.create(DynamoDBEntityNoRange, context, cache)
        then:
            schema.attributeNames().size() == 3
    }

    /**
     * The introspection for Groovy 2,x types does not read the type information beyond the first level. For example,
     * while having <code>Map<String, List<String></code> property then the map type parameters
     * are read but the list parameter is not and defaults to <code>Object</code>
     */
    @IgnoreIf({ GroovySystem.version.startsWith('2') })
    void 'read table schema for groovy class with map property'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntityMapProperty> schema = BeanIntrospectionTableSchema.create(DynamoDBEntityMapProperty, context, cache)
        then:
            schema.attributeNames().size() == 4
    }

    void 'verify converters'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntity> schema = BeanIntrospectionTableSchema.create(DynamoDBEntity, context, cache)
        then:
            schema.itemToMap(new DynamoDBEntity(date: new Date()), ['date']).get('date').s()
    }

}
