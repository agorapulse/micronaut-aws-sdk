/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Specification for testing DefaultDynamoDBService using entity with no range key.
 */
@Testcontainers
class DefaultDynamoDBServiceNoRangeSpec extends Specification {

    @Shared LocalStackContainer localstack = new LocalStackContainer().withServices(LocalStackV2Container.Service.DYNAMODB)

    @AutoCleanup ApplicationContext context

    DynamoDBEntityNoRangeService service

    void setup() {
        context = ApplicationContext.build(
            'aws.region': 'eu-west-1',
            'aws.access-key-id': localstack.accessKey,
            'aws.secret-access-key': localstack.secretKey,
            'aws.dynamodb.endpoint': localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString()
        ).build()

        context.start()

        service = context.getBean(DynamoDBEntityNoRangeService)
    }

    void 'basic CRUD'() {
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

        and:
            service.get('1')
            service.count('1') == 1

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
            service.delete(service.get('1'))
            service.count('1') == 0
            service.deleteByHash('3')
            service.count('3') == 0
            service.deleteByPartition('2')
            service.count('1') == 0
    }

}
