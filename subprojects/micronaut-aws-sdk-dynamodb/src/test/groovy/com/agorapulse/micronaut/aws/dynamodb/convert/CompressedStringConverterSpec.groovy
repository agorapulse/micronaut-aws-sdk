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
package com.agorapulse.micronaut.aws.dynamodb.convert

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBService
import com.agorapulse.micronaut.aws.dynamodb.DynamoDBServiceProvider
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB

@Slf4j
@Testcontainers
class CompressedStringConverterSpec extends Specification {

    @AutoCleanup
    @Shared
    ApplicationContext ctx

    @Shared
    @AutoCleanup
    LocalStackContainer localstack = new LocalStackContainer().withServices(DYNAMODB)

    @Shared
    AmazonDynamoDB amazonDynamoDB

    @Shared
    DynamoDBService<EntityExample> dynamoDBService

    void setupSpec() {
        amazonDynamoDB = AmazonDynamoDBClient.builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(DYNAMODB))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        IDynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB)

        ctx = ApplicationContext.builder().build()
        ctx.registerSingleton(AmazonDynamoDB, amazonDynamoDB)
        ctx.registerSingleton(IDynamoDBMapper, mapper)
        ctx.start()

        dynamoDBService = ctx.getBean(DynamoDBServiceProvider).findOrCreate(EntityExample)
        try {
            dynamoDBService.createTable()
        } catch (AmazonServiceException e) {
            log.warn e.message
        }
    }

    void 'should persist entity with compressed data field'() {
        given:
        String json = this.class.classLoader.getResourceAsStream('example.json').text
        EntityExample entity = new EntityExample(UUID.randomUUID().toString(), json)

        when:
        EntityExample saved = dynamoDBService.save(entity)

        then:
        saved.id == entity.id
        saved.data == entity.data

        and:
        dynamoDBService.get(entity.id).data == entity.data

        when:
        int uncompressedSize = entity.data.length()
        int compressedSize = getRawDynamoDbItem(entity.id).item.get('data').b.array().size()

        log.info 'uncompressed size: {} bytes', uncompressedSize
        log.info 'compressed size: {} bytes', compressedSize

        then:
        compressedSize < uncompressedSize
    }

    private GetItemResult getRawDynamoDbItem(String id) {
        GetItemRequest request = new GetItemRequest()
            .withTableName(EntityExample.simpleName)
            .withKey([id: new AttributeValue().withS(id)])

        return amazonDynamoDB.getItem(request)
    }

}
