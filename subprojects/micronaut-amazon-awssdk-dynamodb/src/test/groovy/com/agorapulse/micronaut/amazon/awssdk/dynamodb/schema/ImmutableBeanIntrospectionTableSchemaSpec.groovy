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

import io.micronaut.context.BeanContext
import io.micronaut.core.convert.ConversionService
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.MetaTableSchemaCache
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

class ImmutableBeanIntrospectionTableSchemaSpec extends Specification {

    MetaTableSchemaCache cache = new MetaTableSchemaCache()

    BeanContext context = Mock {
        findBean(_) >> Optional.empty()
        conversionService >> ConversionService.SHARED
    }

    void 'read table schema for immutable class with @DynamoDbImmutable'() {
        when:
            ImmutableBeanIntrospectionTableSchema<ImmutableEntityWithDynamoDbImmutable> schema =
                ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithDynamoDbImmutable, context, cache)
        then:
            schema.attributeNames().size() == 3 // id, sortKey, ttl
            schema.attributeNames().containsAll(['id', 'sortKey', 'ttl'])

        when:
            ImmutableEntityWithDynamoDbImmutable entity = ImmutableEntityWithDynamoDbImmutable.builder()
                .id(1L)
                .sortKey(2L)
                .build()
            Map<String, AttributeValue> map = schema.itemToMap(entity, true)
        then:
            map.get('id').n() == '1'
            map.get('sortKey').n() == '2'
            map.get('ttl').n() != null

        when:
            ImmutableEntityWithDynamoDbImmutable loaded = schema.mapToItem(map)
        then:
            loaded.id == 1L
            loaded.sortKey == 2L
    }

    void 'read table schema for immutable class with @Immutable annotation'() {
        when:
            ImmutableBeanIntrospectionTableSchema<ImmutableEntityWithCustomAnnotation> schema =
                ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithCustomAnnotation, context, cache)
        then:
            schema.attributeNames().size() == 4 // id, sortKey, created, ttl
            schema.attributeNames().containsAll(['id', 'sortKey', 'created', 'ttl'])

        when:
            Instant now = Instant.now()
            ImmutableEntityWithCustomAnnotation entity = ImmutableEntityWithCustomAnnotation.builder()
                .id(1L)
                .sortKey(2L)
                .created(now)
                .build()
            Map<String, AttributeValue> map = schema.itemToMap(entity, true)
        then:
            map.get('id').n() == '1'
            map.get('sortKey').n() == '2'
            map.get('created').s() != null
            map.get('ttl').n() != null

        when:
            ImmutableEntityWithCustomAnnotation loaded = schema.mapToItem(map)
        then:
            loaded.id == 1L
            loaded.sortKey == 2L
            loaded.created == now
    }

    void 'read table schema for immutable record'() {
        when:
            ImmutableBeanIntrospectionTableSchema<ImmutableEntityRecord> schema =
                ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityRecord, context, cache)
        then:
            schema.attributeNames().size() == 4 // accountId, subId, name, createdDate

        when:
            Instant now = Instant.now()
            ImmutableEntityRecord entity = ImmutableEntityRecord.builder()
                .accountId('test-account')
                .subId(123)
                .name('John Doe')
                .createdDate(now)
                .build()
            Map<String, AttributeValue> map = schema.itemToMap(entity, true)
        then:
            map.get('accountId').s() == 'test-account'
            map.get('subId').n() == '123'
            map.get('name').s() == 'John Doe'
            map.get('createdDate').s() != null

        when:
            ImmutableEntityRecord loaded = schema.mapToItem(map)
        then:
            loaded.accountId() == 'test-account'
            loaded.subId() == 123
            loaded.name() == 'John Doe'
            loaded.createdDate() == now
    }

    void 'read table schema for immutable class with nested beans'() {
        when:
            ImmutableBeanIntrospectionTableSchema.create(ImmutablePersonEntity, context, cache)
        then:
            thrown(IllegalArgumentException)
    }

    void 'ttl is computed correctly for immutable entity with TimeToLive annotation'() {
        given:
            ImmutableBeanIntrospectionTableSchema<ImmutableEntityWithDynamoDbImmutable> schema =
                ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithDynamoDbImmutable, context, cache)

        when:
            long secondsBefore = (long) (System.currentTimeMillis() / 1000L)
            ImmutableEntityWithDynamoDbImmutable entity = ImmutableEntityWithDynamoDbImmutable.builder()
                .id(1L)
                .sortKey(2L)
                .build()
            long ttl = schema.itemToMap(entity, ['ttl']).get('ttl').n().toLong()
            long secondsAfter = (long) (System.currentTimeMillis() / 1000L)
        then:
            Duration expected = Duration.ofDays(30)
            ttl >= secondsBefore + expected.seconds
            ttl <= secondsAfter + expected.seconds
    }

    void 'ttl is computed correctly for immutable entity with field-level TimeToLive annotation'() {
        given:
            ImmutableBeanIntrospectionTableSchema<ImmutableEntityWithCustomAnnotation> schema =
                ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithCustomAnnotation, context, cache)

        when:
            Instant refTime = Instant.now()
            ImmutableEntityWithCustomAnnotation entity = ImmutableEntityWithCustomAnnotation.builder()
                .id(1L)
                .sortKey(2L)
                .created(refTime)
                .build()
            long ttl = schema.itemToMap(entity, ['ttl']).get('ttl').n().toLong()
        then:
            ttl == (refTime + Duration.ofDays(45)).epochSecond
    }

    void 'verify builder pattern works correctly'() {
        when:
            ImmutableEntityWithDynamoDbImmutable entity1 = ImmutableEntityWithDynamoDbImmutable.builder()
                .id(1L)
                .sortKey(2L)
                .build()

            ImmutableEntityWithDynamoDbImmutable entity2 = ImmutableEntityWithDynamoDbImmutable.builder()
                .id(1L)
                .sortKey(2L)
                .build()
        then:
            entity1 == entity2
            entity1.hashCode() == entity2.hashCode()
            entity1.toString().contains('id=1')
            entity1.toString().contains('sortKey=2')
    }

    void 'verify record pattern works correctly'() {
        when:
            Instant now = Instant.now()
            ImmutableEntityRecord record1 = ImmutableEntityRecord.builder()
                .accountId('test')
                .subId(123)
                .name('John')
                .createdDate(now)
                .build()

            ImmutableEntityRecord record2 = new ImmutableEntityRecord('test', 123, 'John', now)
        then:
            record1 == record2
            record1.hashCode() == record2.hashCode()
            record1.accountId() == 'test'
            record1.subId() == 123
            record1.name() == 'John'
            record1.createdDate() == now
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    void 'builder class must be annotated with @Introspected'() {
        when:
            // This would fail in a real scenario if builder wasn't annotated
            ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithDynamoDbImmutable, context, cache)
        then:
            noExceptionThrown()
    }

    void 'immutable class must be annotated with @Introspected'() {
        when:
            // This would fail in a real scenario if the immutable class wasn't annotated
            ImmutableBeanIntrospectionTableSchema.create(ImmutableEntityWithDynamoDbImmutable, context, cache)
        then:
            noExceptionThrown()
    }

}
