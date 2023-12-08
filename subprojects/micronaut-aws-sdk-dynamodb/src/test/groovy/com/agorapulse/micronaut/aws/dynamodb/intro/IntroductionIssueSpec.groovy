/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.aws.dynamodb.intro

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBService
import com.agorapulse.micronaut.aws.dynamodb.DynamoDBServiceProvider
import groovy.transform.CompileStatic
import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import jakarta.inject.Singleton

/**
 * Tests that guarantees that more than one DynamoDB service can be injected into the service which was previously
 * not working due the following bug.
 *
 * @see https://github.com/micronaut-projects/micronaut-core/issues/1851
 */
@MicronautTest
class IntroductionIssueSpec extends Specification {

    @Inject ApplicationContext context
    @Inject DynamoDBServiceProvider serviceProvider

    void setup() {
        DynamoDBService<IntroProblemEntity> first = serviceProvider.findOrCreate(IntroProblemEntity)
        first.save(new IntroProblemEntity('hash1'))

        DynamoDBService<IntroProblemEntity2> second = serviceProvider.findOrCreate(IntroProblemEntity2)
        second.save(new IntroProblemEntity2(hashKey: 'hash2'))
    }

    void 'can inject two data services'() {
        given:
            Injected service = context.getBean(Injected)
        when:
            service.loadEntities()
        then:
            noExceptionThrown()
    }

}

@Singleton
@CompileStatic
class Injected {

    final IntroProblemEntity2DBService introProblemEntity2DBService
    final IntroProblemEntityDBService introProblemEntityDBService

    Injected(IntroProblemEntity2DBService introProblemEntity2DBService, IntroProblemEntityDBService introProblemEntityDBService) {
        this.introProblemEntity2DBService = introProblemEntity2DBService
        this.introProblemEntityDBService = introProblemEntityDBService
    }

    void loadEntities() {
        assert introProblemEntityDBService.load('hash') == null
        assert introProblemEntity2DBService.load('hash') == null
    }

}
