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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.KeyConditionCollector
import spock.lang.Specification

class FilterOperatorSpec extends Specification {

    void "operator #operator is not supported for sort keys"(Filter.Operator operator) {
        when:
            operator.apply(Mock(KeyConditionCollector), 'sortKey', 'value', null)
        then:
            thrown(UnsupportedOperationException)
        where:
            operator << (Filter.Operator.values() - [
                Filter.Operator.EQ,
                Filter.Operator.LE,
                Filter.Operator.LT,
                Filter.Operator.GE,
                Filter.Operator.GT,
                Filter.Operator.BETWEEN,
                Filter.Operator.BEGINS_WITH,
            ])
    }

    void 'inList is called for operator IN_LIST'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.IN_LIST.apply(collector, 'key', [1, 2, 3], null)
        then:
            1 * collector.inList('key', [1, 2, 3])
    }

    void 'ne is called for operator NE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.NE.apply(collector, 'key', 'value', null)
        then:
            1 * collector.ne('key', 'value')
    }

    void 'le is called for operator LE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.LE.apply(collector, 'key', 'value', null)
            Filter.Operator.LE.apply(keyConditionCollector, 'key', 'value', null)

        then:
            1 * collector.le('key', 'value')
            1 * keyConditionCollector.le('value')
    }

    void 'lt is called for operator LT'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.LT.apply(collector, 'key', 'value', null)
            Filter.Operator.LT.apply(keyConditionCollector, 'key', 'value', null)

        then:
            1 * collector.lt('key', 'value')
            1 * keyConditionCollector.lt('value')
    }

    void 'ge is called for operator GE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.GE.apply(collector, 'key', 'value', null)
            Filter.Operator.GE.apply(keyConditionCollector, 'key', 'value', null)

        then:
            1 * collector.ge('key', 'value')
            1 * keyConditionCollector.ge('value')
    }

    void 'gt is called for operator GT'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.GT.apply(collector, 'key', 'value', null)
            Filter.Operator.GT.apply(keyConditionCollector, 'key', 'value', null)

        then:
            1 * collector.gt('key', 'value')
            1 * keyConditionCollector.gt('value')
    }

    void 'beginsWith is called for operator BEGINS_WITH'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.BEGINS_WITH.apply(collector, 'key', 'value', null)
            Filter.Operator.BEGINS_WITH.apply(keyConditionCollector, 'key', 'value', null)
        then:
            1 * collector.beginsWith('key', 'value')
            1 * keyConditionCollector.beginsWith('value')
    }

    void 'between is called for operator BETWEEN'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.BETWEEN.apply(collector, 'key', 'first', 'second')
            Filter.Operator.BETWEEN.apply(keyConditionCollector, 'key', 'first', 'second')
        then:
            1 * collector.between('key', 'first', 'second')
            1 * keyConditionCollector.between('first', 'second')
    }

    void 'sizeEq is called for operator SIZE_EQ'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_EQ.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeEq('key', 5)
    }

    void 'sizeGt is called for operator SIZE_GT'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_GT.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeGt('key', 5)
    }

    void 'sizeGe is called for operator SIZE_GE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_GE.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeGe('key', 5)
    }

    void 'sizeLt is called for operator SIZE_LT'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_LT.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeLt('key', 5)
    }

    void 'sizeLe is called for operator SIZE_LE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_LE.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeLe('key', 5)
    }

    void 'sizeNe is called for operator SIZE_NE'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.SIZE_NE.apply(collector, 'key', 5, null)
        then:
            1 * collector.sizeNe('key', 5)
    }

    void 'contains is called for operator CONTAINS'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.CONTAINS.apply(collector, 'key', 'value', null)
        then:
            1 * collector.contains('key', 'value')
    }

    void 'notContains is called for operator NOT_CONTAINS'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.NOT_CONTAINS.apply(collector, 'key', 'value', null)
        then:
            1 * collector.notContains('key', 'value')
    }

    void 'typeOf is called for operator TYPE_OF'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.TYPE_OF.apply(collector, 'key', String, null)
        then:
            1 * collector.typeOf('key', String)
    }

    void 'eq is called for simple object and EQ'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
            KeyConditionCollector keyConditionCollector = Mock(KeyConditionCollector)
        when:
            Filter.Operator.EQ.apply(collector, 'key', 'value', null)
            Filter.Operator.EQ.apply(keyConditionCollector, 'key', 'value', null)
        then:
            1 * collector.eq('key', 'value')
            1 * keyConditionCollector.eq('value')
    }

    void 'inList is called for collection and EQ'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.EQ.apply(collector, 'key', [1, 2, 3], null)
        then:
            1 * collector.inList('key', [1, 2, 3])
    }

    void 'inList is called for an array and EQ'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.EQ.apply(collector, 'key', [1, 2, 3] as Integer[], null)
        then:
            1 * collector.inList('key', [1, 2, 3])
    }

    void 'isNull is called for eq operator and null value'() {
        given:
            FilterConditionCollector collector = Mock(FilterConditionCollector)
        when:
            Filter.Operator.EQ.apply(collector, 'key', null, null)
        then:
            1 * collector.isNull('key')
    }

}
