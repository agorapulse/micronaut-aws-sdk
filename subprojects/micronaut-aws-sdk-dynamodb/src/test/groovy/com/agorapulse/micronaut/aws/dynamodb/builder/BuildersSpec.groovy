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
package com.agorapulse.micronaut.aws.dynamodb.builder

import com.agorapulse.micronaut.aws.dynamodb.DynamoDBEntity
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static com.agorapulse.micronaut.aws.dynamodb.builder.Builders.*

/**
 * Tests for builders.
 */
@MicronautTest
@CompileDynamic
@SuppressWarnings('NoJavaUtilDate')
class BuildersSpec extends Specification {

    @Inject IDynamoDBMapper mapper

    void 'inconsistent desc query'() {
        when:
            DynamoDBQueryExpression<DynamoDBEntity> expression = createEntityQueryInconsistentDesc().resolveExpression(mapper)
        then:
            noExceptionThrown()

            !expression.consistentRead
            !expression.scanIndexForward
            expression.hashKeyValues.parentId == 'abc'
            expression.indexName == DynamoDBEntity.RANGE_INDEX
            expression.queryFilter.size() == 2
            expression.conditionalOperator == 'OR'
            expression.projectionExpression == 'number,date'
    }

    void 'consistent desc query'() {
        when:
            DynamoDBQueryExpression<DynamoDBEntity> expression = createEntityQueryConsistentAsc().resolveExpression(mapper)
        then:
            noExceptionThrown()

            expression.consistentRead
            expression.scanIndexForward
            expression.hashKeyValues.parentId == 'abc'
            expression.indexName == DynamoDBEntity.RANGE_INDEX
            expression.exclusiveStartKey
            expression.limit == 100
    }

    void 'inconsistent desc scan'() {
        when:
            DynamoDBScanExpression expression = createEntityScanInconsistentDesc().resolveExpression(mapper)
        then:
            noExceptionThrown()

            !expression.consistentRead
            expression.indexName == DynamoDBEntity.RANGE_INDEX
            expression.scanFilter.size() == 2
            expression.conditionalOperator == 'OR'
            expression.projectionExpression == 'number,date'
    }

    void 'consistent desc scan'() {
        when:
            DynamoDBScanExpression expression = createEntityScanConsistentAsc().resolveExpression(mapper)
        then:
            noExceptionThrown()

            expression.consistentRead
            expression.indexName == DynamoDBEntity.RANGE_INDEX
            expression.exclusiveStartKey
            expression.limit == 100
    }

    @CompileStatic
    private static QueryBuilder<DynamoDBEntity> createEntityQueryInconsistentDesc() {
        return query(DynamoDBEntity) {
            hash 'abc'
            sort desc
            inconsistent read
            index DynamoDBEntity.RANGE_INDEX
            range {
                le(DynamoDBEntity.RANGE_INDEX, 'def')
            }
            or {
                gt DynamoDBEntity.DATE_INDEX, new Date()
                le 'number', 100
            }
            only 'number', 'date'
        }
    }

    @CompileStatic
    private static QueryBuilder<DynamoDBEntity> createEntityQueryConsistentAsc() {
        return query(DynamoDBEntity) {
            hash new DynamoDBEntity(parentId: 'abc')
            sort asc
            consistent read
            configure {
                it.indexName = DynamoDBEntity.RANGE_INDEX
            }
            page 100
            offset 'abc', 'def'
        }
    }

    @CompileStatic
    private static ScanBuilder<DynamoDBEntity> createEntityScanInconsistentDesc() {
        return scan(DynamoDBEntity) {
            inconsistent read
            index DynamoDBEntity.RANGE_INDEX
            or {
                gt DynamoDBEntity.DATE_INDEX, new Date()
                le 'number', 100
            }
            only 'number', 'date'
        }
    }

    @CompileStatic
    private static ScanBuilder<DynamoDBEntity> createEntityScanConsistentAsc() {
        return scan(DynamoDBEntity) {
            consistent read
            configure {
                it.indexName = DynamoDBEntity.RANGE_INDEX
            }
            page 100
            offset 'abc', 'def'
        }
    }

}
