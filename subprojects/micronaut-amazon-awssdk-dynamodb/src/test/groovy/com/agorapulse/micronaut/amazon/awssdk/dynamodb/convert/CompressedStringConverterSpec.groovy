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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceProvider
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDbService
import groovy.util.logging.Slf4j
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import spock.lang.Shared
import spock.lang.Specification

import jakarta.inject.Inject

@Slf4j
@MicronautTest
class CompressedStringConverterSpec extends Specification {

    @Inject DynamoDBServiceProvider dynamoDBServiceProvider
    @Inject DynamoDbClient dynamoDbClient
    @Shared DynamoDbService<EntityExample> dynamoDbService

    void setup() {
        dynamoDbService = dynamoDBServiceProvider.findOrCreate(EntityExample)
    }

    void 'should persist entity with compressed data field'() {
        given:
        dynamoDbService.createTable()

        and:
        String json = this.class.classLoader.getResourceAsStream('example.json').text
        EntityExample entity = new EntityExample(UUID.randomUUID().toString(), json)

        when:
        EntityExample saved = dynamoDbService.save(entity)

        then:
        saved.id == entity.id
        saved.data == entity.data

        and:
        dynamoDbService.get(Key.builder().partitionValue(entity.id).build()).data == entity.data

        when:
        int uncompressedSize = entity.data.length()
        int compressedSize = getRawDynamoDbItem(entity.id).item().get('data').b().asByteArray().size()

        log.info 'uncompressed size: {} bytes', uncompressedSize
        log.info 'compressed size: {} bytes', compressedSize

        then:
        compressedSize < uncompressedSize
    }

    private GetItemResponse getRawDynamoDbItem(String id) {
        GetItemRequest request = GetItemRequest
            .builder()
            .key([id: AttributeValue.builder().s(id).build()])
            .tableName(EntityExample.simpleName)
            .build() as GetItemRequest

        return dynamoDbClient.getItem(request)
    }

}
