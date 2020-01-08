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
package com.agorapulse.micronaut.aws.dynamodb.builder

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.micronaut.aws.dynamodb.DynamoDBEntity
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import spock.lang.Specification

/**
 * Tests for creating conditions.
 */
@SuppressWarnings([
    'AbcMetric',
    'UnnecessaryGetter',
])
class RangeConditionCollectorSpec extends Specification {

    private static final String ARG_1 = 'foo'
    private static final String ARG_2 = 'bar'

    IDynamoDBMapper mapper = DynamoDB.createMapper(Dru.steal(this))

    void 'range conditions created'() {
        when:
            Condition eqCondition = condition { eq(ARG_1) }
        then:
            eqCondition.comparisonOperator == ComparisonOperator.EQ.toString()
            eqCondition.attributeValueList.first().s == ARG_1

        when:
            Condition neCondition = condition { ne(ARG_1) }
        then:
            neCondition.comparisonOperator == ComparisonOperator.NE.toString()
            neCondition.attributeValueList.first().s == ARG_1

        when:
            Condition leCondition = condition { le(ARG_1) }
        then:
            leCondition.comparisonOperator == ComparisonOperator.LE.toString()
            leCondition.attributeValueList.first().s == ARG_1

        when:
            Condition ltCondition = condition { lt(ARG_1) }
        then:
            ltCondition.comparisonOperator == ComparisonOperator.LT.toString()
            ltCondition.attributeValueList.first().s == ARG_1

        when:
            Condition geCondition = condition { ge(ARG_1) }
        then:
            geCondition.comparisonOperator == ComparisonOperator.GE.toString()
            geCondition.attributeValueList.first().s == ARG_1

        when:
            Condition gtCondition = condition { gt(ARG_1) }
        then:
            gtCondition.comparisonOperator == ComparisonOperator.GT.toString()
            gtCondition.attributeValueList.first().s == ARG_1

        when:
            Condition betweenCondition = condition { between(ARG_1, ARG_2) }
        then:
            betweenCondition.comparisonOperator == ComparisonOperator.BETWEEN.toString()
            betweenCondition.attributeValueList.first().s == ARG_1
            betweenCondition.attributeValueList.last().s == ARG_2

        when:
            Condition isNotNullCondition = condition { isNotNull() }
        then:
            isNotNullCondition.comparisonOperator == ComparisonOperator.NOT_NULL.toString()

        when:
            Condition isNullCondition = condition { isNull() }
        then:
            isNullCondition.comparisonOperator == ComparisonOperator.NULL.toString()

        when:
            Condition containsCondition = condition { contains(ARG_1) }
        then:
            containsCondition.comparisonOperator == ComparisonOperator.CONTAINS.toString()
            containsCondition.attributeValueList.first().s == ARG_1

        when:
            Condition notContainsCondition = condition { notContains(ARG_1) }
        then:
            notContainsCondition.comparisonOperator == ComparisonOperator.NOT_CONTAINS.toString()
            notContainsCondition.attributeValueList.first().s == ARG_1

        when:
            Condition beginsWithCondition = condition { beginsWith(ARG_1) }
        then:
            beginsWithCondition.comparisonOperator == ComparisonOperator.BEGINS_WITH.toString()
            beginsWithCondition.attributeValueList.first().s == ARG_1

        when:
            Condition inListOfCondition = condition { inListOf(ARG_1, ARG_2) }
        then:
            inListOfCondition.comparisonOperator == ComparisonOperator.IN.toString()
            inListOfCondition.attributeValueList.first().s == ARG_1
            inListOfCondition.attributeValueList.last().s == ARG_2

        when:
            Condition inListCondition = condition { inList([ARG_1, ARG_2]) }
        then:
            inListCondition.comparisonOperator == ComparisonOperator.IN.toString()
            inListCondition.attributeValueList.first().s == ARG_1
            inListCondition.attributeValueList.last().s == ARG_2
    }

    Condition condition(
        @DelegatesTo(type = 'com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>', strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString, options = 'com.agorapulse.micronaut.aws.dynamodb.builder.RangeConditionCollector<T>')
            Closure<RangeConditionCollector<DynamoDBEntity>> conditions
    ) {
        return Builders.scan(DynamoDBEntity).filter(conditions).resolveExpression(mapper).scanFilter['id']
    }

}
