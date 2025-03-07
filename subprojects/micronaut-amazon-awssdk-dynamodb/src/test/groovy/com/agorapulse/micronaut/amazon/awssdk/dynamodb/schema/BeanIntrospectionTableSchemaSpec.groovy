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
import io.micronaut.core.convert.ConversionService
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BeanIntrospectionTableSchemaSpec extends Specification {

    MetaTableSchemaCache cache = new MetaTableSchemaCache()

    BeanContext context = Mock {
        findBean(_) >> Optional.empty()
        getConversionService() >> ConversionService.SHARED
    }

    void 'read table schema for java class'() {
        when:
            BeanIntrospectionTableSchema<DynamoDBEntity> schema = BeanIntrospectionTableSchema.create(DynamoDBEntity, context, cache)
        then:
            schema.attributeNames().size() == 11
    }

    <T> void '#attribute attribute is added to the class #type when TimeToLive annotation is used with duration #duration'(
        Class<T> type, String attribute, Duration duration, T instance
    ) {
        when:
            BeanIntrospectionTableSchema<T> schema = BeanIntrospectionTableSchema.create(type, context, cache)
        then:
            schema.attributeNames().size() == 3
            schema.attributeNames().contains(attribute)

        when:
            long secondsBefore = (long) (System.currentTimeMillis() / 1000L)
            long ttl = schema.itemToMap(instance, [attribute]).get(attribute).n().toLong()
            long secondsAfter = (long) (System.currentTimeMillis() / 1000L)
        then:
            verifyAll {
                ttl >= secondsBefore + duration.seconds
                ttl <= secondsAfter + duration.seconds
            }

        where:
            type                | attribute   | duration             | instance
            EntityWithTtl       | 'ttl'       | Duration.ofDays(30)  | new EntityWithTtl(id: 1L, sortKey: 2L)
            EntityWithCustomTtl | 'customTtl' | Duration.ofDays(365) | new EntityWithCustomTtl(id: 1L, sortKey: 2L)
    }

    void 'ttl is added to the class with TimeToLive annotation on the instant field'() {
        when:
            BeanIntrospectionTableSchema<EntityWithTtlOnInstantField> schema = BeanIntrospectionTableSchema.create(EntityWithTtlOnInstantField, context, cache)
        then:
            schema.attributeNames().size() == 4
            schema.attributeNames().contains('ttl')

        when:
            Instant refTime = Instant.now()
            long ttl = schema.itemToMap(new EntityWithTtlOnInstantField(id: 1L, sortKey: 2L, created: refTime), ['ttl']).get('ttl').n().toLong()
        then:
            ttl == (refTime + Duration.ofDays(45)).epochSecond
    }

    void 'ttl is added to the class with TimeToLive annotation on the string field'() {
        when:
            BeanIntrospectionTableSchema<EntityWithTtlOnStringField> schema = BeanIntrospectionTableSchema.create(EntityWithTtlOnStringField, context, cache)
        then:
            schema.attributeNames().size() == 4
            schema.attributeNames().contains('ttl')

        when:
            String refTime = '2020-01-01T00'
            long ttl = schema.itemToMap(new EntityWithTtlOnStringField(id: 1L, sortKey: 2L, created: refTime), ['ttl']).get('ttl').n().toLong()
        then:
            ttl == (LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant() + Duration.ofDays(17)).epochSecond
    }

    void 'ttl is added to the class with TimeToLive annotation on the long field'() {
        when:
            BeanIntrospectionTableSchema<EntityWithTtlOnLongField> schema = BeanIntrospectionTableSchema.create(EntityWithTtlOnLongField, context, cache)
        then:
            schema.attributeNames().size() == 4
            schema.attributeNames().contains('ttl')

        when:
            Long refTime = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            long ttl = schema.itemToMap(new EntityWithTtlOnLongField(id: 1L, sortKey: 2L, created: refTime), ['ttl']).get('ttl').n().toLong()
        then:
            ttl == (LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant() + Duration.ofDays(3)).epochSecond
    }

    void 'ttl field must be convertable'() {
        when:
            BeanIntrospectionTableSchema<EntityWithTtlOnDateField> schema = BeanIntrospectionTableSchema.create(EntityWithTtlOnDateField, context, cache)
        then:
            thrown(IllegalArgumentException)
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
