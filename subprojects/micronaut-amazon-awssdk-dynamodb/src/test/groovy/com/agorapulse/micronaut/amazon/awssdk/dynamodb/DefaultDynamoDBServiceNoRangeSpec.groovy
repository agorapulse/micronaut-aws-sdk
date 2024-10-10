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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.reactivex.Flowable
import spock.lang.Specification

import jakarta.inject.Inject

/**
 * Specification for testing DefaultDynamoDBService using entity with no range key.
 */
@MicronautTest
@Property(name = 'aws.dynamodb.client', value = ClientBuilderProvider.URL_CONNECTION)
@Property(name = 'aws.dynamodb.async-client', value = ClientBuilderProvider.NETTY)
class DefaultDynamoDBServiceNoRangeSpec extends Specification {

    @Inject DynamoDBEntityNoRangeService service

    void 'basic CRUD'() {
        when:
            service.save(new DynamoDBEntityNoRange(parentId: '1'))
            service.save(new DynamoDBEntityNoRange(parentId: '2'))
            service.saveAll([
                new DynamoDBEntityNoRange(parentId: '3'),
                new DynamoDBEntityNoRange(parentId: '4')]
            ).collectList().block()
            service.saveAll(Flowable.just(
                new DynamoDBEntityNoRange(parentId: '5'),
                new DynamoDBEntityNoRange(parentId: '6')
            )).toList().blockingGet()

        then:
            noExceptionThrown()

        and:
            service.get('1')
            service.count('1') == 1
            service.get('2')
            service.count('2') == 1
            service.get('3')
            service.count('3') == 1
            service.get('4')
            service.count('4') == 1
            service.get('5')
            service.count('5') == 1
            service.get('6')
            service.count('6') == 1
            service.getAll(['1', '2', '3', '4', '5', '6']).parentId == ['1', '2', '3', '4', '5', '6']

        when:
            service.increment('1')
            service.increment('1')
            service.increment('1')
            service.decrement('1')
        then:
            service.get('1').number == 2

        and:
            service.query('1').size() == 1
            service.get('1').number == 2

        when:
            service.deleteNumber('1')
        then:
            !service.get('1').number

        and:
            service.count('1') == 1
            service.delete(service.get('1'))
            service.count('1') == 0
            service.query('3').size() == 1
            service.count('3') == 1
            service.deleteByHash('3')
            service.count('3') == 0
            service.deleteByPartition('2')
            service.count('1') == 0
    }

}
