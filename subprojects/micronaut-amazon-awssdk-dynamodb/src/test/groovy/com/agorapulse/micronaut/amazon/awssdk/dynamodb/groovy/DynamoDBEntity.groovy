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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.groovy

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Projection
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondaryPartitionKey
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondarySortKey
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import software.amazon.awssdk.services.dynamodb.model.ProjectionType

@Canonical
@Introspected                                                                           // <1>
@CompileStatic
class DynamoDBEntity {

    public static final String DATE_INDEX = 'date'
    public static final String RANGE_INDEX = 'rangeIndex'
    public static final String GLOBAL_INDEX = 'globalIndex'

    @PartitionKey String parentId                                                       // <2>
    @SortKey String id                                                                  // <3>

    @SecondarySortKey(indexNames = RANGE_INDEX)                                         // <4>
    String rangeIndex

    @Projection(ProjectionType.ALL)                                                     // <5>
    @SecondarySortKey(indexNames = DATE_INDEX)
    Date date

    Integer number = 0

    @Projection(ProjectionType.ALL)
    @SecondaryPartitionKey(indexNames = GLOBAL_INDEX)                                   // <6>
    String getGlobalIndex() {
        return "$parentId:$id"
    }

}
