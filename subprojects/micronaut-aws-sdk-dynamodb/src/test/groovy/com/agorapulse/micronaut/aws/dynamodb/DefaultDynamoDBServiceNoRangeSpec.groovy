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
package com.agorapulse.micronaut.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import spock.lang.Stepwise

import jakarta.inject.Inject

/**
 * Specification for testing DefaultDynamoDBService using entity with no range key.
 */
@Stepwise
@MicronautTest
class DefaultDynamoDBServiceNoRangeSpec extends Specification {

    @Inject AmazonDynamoDB amazonDynamoDB
    @Inject DynamoDBServiceProvider provider
    DynamoDBService<DynamoDBEntityNoRange> service

    void setup() {
        service = provider.findOrCreate(DynamoDBEntityNoRange)
    }

    void 'required DynamoDB table annotation'() {
        when:
            new DefaultDynamoDBService<>(amazonDynamoDB, new DynamoDBMapper(amazonDynamoDB), Map)
        then:
            thrown RuntimeException
    }

    void 'check metadata'() {
        expect:
            service.hashKeyName == 'parentId'
            service.hashKeyClass == String
            !service.rangeKeyName
            !service.rangeKeyClass
            service.newInstance instanceof DynamoDBEntityNoRange
    }

    void 'new table'() {
        when:
            CreateTableResult result = service.createTable()
        then:
            result.tableDescription.tableName == 'entityNoRange'

        when:
            CreateTableResult none = service.createTable()
        then:
            !none
    }

    void 'save items'() {
        when:
            service.save(new DynamoDBEntityNoRange(parentId: '1'))
            service.save(new DynamoDBEntityNoRange(parentId: '2'))
            service.saveAll([
                new DynamoDBEntityNoRange(parentId: '3'),
                new DynamoDBEntityNoRange(parentId: '4')])
            service.saveAll(
                new DynamoDBEntityNoRange(parentId: '5'),
                new DynamoDBEntityNoRange(parentId: '6')
            )

        then:
            noExceptionThrown()
    }

    void 'get items'() {
        expect:
            service.get('1')
    }

    void 'count items'() {
        expect:
            service.count('1') == 1
    }

    void 'increment and decrement'() {
        when:
            service.increment('1', 'number')
            service.increment('1', 'number')
            service.increment('1', 'number')
            service.decrement('1', 'number')
        then:
            service.get('1').number == 2
    }

    void 'query items'() {
        expect:
            service.query('1').count == 1
            service.query(new DynamoDBQueryExpression().withHashKeyValues(new DynamoDBEntityNoRange(parentId: '1'))).size() == 1
    }

    void 'delete attribute'() {
        expect:
            service.get('1').number == 2
        when:
            service.deleteItemAttribute('1', 'number')
        then:
            !service.get('1').number
    }

    void 'delete items'() {
        expect:
            service.delete(service.get('1'))
            service.count('1') == 0
            service.deleteByHash('3')
            service.count('3') == 0
    }

}
