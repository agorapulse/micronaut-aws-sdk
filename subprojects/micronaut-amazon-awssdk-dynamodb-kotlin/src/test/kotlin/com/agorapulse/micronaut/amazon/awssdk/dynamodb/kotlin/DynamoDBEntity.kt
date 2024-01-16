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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*
import io.micronaut.core.annotation.Introspected
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import java.util.*

@Introspected                                                                           // <1>
class DynamoDBEntity {

    @PartitionKey                                                                       // <2>
    var parentId: String? = null

    @SortKey                                                                            // <3>
    var id: String? = null

    @SecondarySortKey(indexNames = [RANGE_INDEX])                                       // <4>
    var rangeIndex: String? = null

    @SecondarySortKey(indexNames = [DATE_INDEX])                                        // <5>
    @Projection(ProjectionType.ALL)
    var date: Date? = null

    var number = 0

    var map: Map<String, List<String>>? = null

    @SecondaryPartitionKey(indexNames = [GLOBAL_INDEX])                                 // <6>
    @Projection(ProjectionType.ALL)
    fun getGlobalIndex(): String {
        return "$parentId:$id"
    }

    companion object {
        const val DATE_INDEX = "date"
        const val RANGE_INDEX = "rangeIndex"
        const val GLOBAL_INDEX = "globalIndex"
    }
}
