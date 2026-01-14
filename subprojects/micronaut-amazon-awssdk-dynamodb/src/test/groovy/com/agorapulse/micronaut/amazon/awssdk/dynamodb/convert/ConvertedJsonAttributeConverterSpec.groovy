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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceProvider
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDbService
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Options
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import spock.lang.Specification

@MicronautTest
class ConvertedJsonAttributeConverterSpec extends Specification {

    @Inject DynamoDBServiceProvider dynamoDBServiceProvider
    @Inject DynamoDbClient dynamoDbClient
    @Inject ObjectMapper objectMapper

    DynamoDbService<ConvertedJsonEntityExample> dynamoDbService

    void setup() {
        dynamoDbService = dynamoDBServiceProvider.findOrCreate(ConvertedJsonEntityExample)
    }

    void 'should persist entity with options stored as JSON'() {
        given:
            ConvertedJsonEntityExample entity = new ConvertedJsonEntityExample(
                UUID.randomUUID().toString(),
                new Options(one: '1', two: '2')
            )
        when:
            ConvertedJsonEntityExample saved = dynamoDbService.save(entity)
        then:
            saved.id == entity.id
            saved.options == entity.options
        when:
            ConvertedJsonEntityExample loaded = dynamoDbService.get(Key.builder().partitionValue(entity.id).build())
        then:
            loaded.id == entity.id
            loaded.options == entity.options

        when:
            String json = RawDataUtil.getRawDynamoDbItem(dynamoDbClient, ConvertedJsonEntityExample, entity.id).options.s()
            Options options = objectMapper.readValue(json, Options)
        then:
            options == entity.options
    }

    void 'should persist entity with null options'() {
        given:
            ConvertedJsonEntityExample entity = new ConvertedJsonEntityExample(
                UUID.randomUUID().toString(),
                null
            )
        when:
            ConvertedJsonEntityExample saved = dynamoDbService.save(entity)
        then:
            saved.id == entity.id
            !saved.options

        when:
            ConvertedJsonEntityExample loaded = dynamoDbService.get(Key.builder().partitionValue(entity.id).build())
        then:
            loaded.id == entity.id
            loaded.options == entity.options

            !RawDataUtil.getRawDynamoDbItem(dynamoDbClient, ConvertedJsonEntityExample, entity.id).options
    }

}
