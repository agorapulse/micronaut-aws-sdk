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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*
import org.reactivestreams.Publisher
import java.util.*

// tag::all[]
// tag::header[]
@Service(value = DynamoDBEntity::class, tableName = "DynamoDBJava")                     // <1>
interface DynamoDBEntityService {
    // end::header[]

    fun get(@PartitionKey parentId: String?, @SortKey id: String?): DynamoDBEntity?
    fun load(@PartitionKey parentId: String?, @SortKey id: String?): DynamoDBEntity?
    fun getAll(hash: String?, rangeKeys: List<String?>?): List<DynamoDBEntity?>?
    fun getAll(hash: String?, vararg rangeKeys: String?): List<DynamoDBEntity?>?
    fun loadAll(hash: String?, rangeKeys: List<String?>?): List<DynamoDBEntity?>?
    fun loadAll(hash: String?, vararg rangeKeys: String?): List<DynamoDBEntity?>?
    fun save(entity: DynamoDBEntity?): DynamoDBEntity?
    fun saveAll(vararg entities: DynamoDBEntity?): List<DynamoDBEntity?>?
    fun saveAll(entities: Iterable<DynamoDBEntity?>?): List<DynamoDBEntity?>?
    fun count(hashKey: String?): Int
    fun count(hashKey: String?, rangeKey: String?): Int

    class EqRangeIndex : QueryFunction<DynamoDBEntity>({ args: Map<String, Any> ->
        partitionKey(args.get("hashKey"))
        index(DynamoDBEntity.RANGE_INDEX)
        sortKey {
            eq(args["rangeKey"])
        }
    })

    @Query(EqRangeIndex::class)
    fun countByRangeIndex(hashKey: String?, rangeKey: String?): Int

    class BetweenDateIndex : QueryFunction<DynamoDBEntity>({ args: Map<String, Any> ->
        index(DynamoDBEntity.DATE_INDEX)
        partitionKey(args["hashKey"])
        sortKey { between(args["after"], args["before"]) }
        page(1)
    })

    @Query(BetweenDateIndex::class)
    fun countByDates(hashKey: String?, after: Date?, before: Date?): Int

    fun query(hashKey: String?): Publisher<DynamoDBEntity?>?
    fun query(hashKey: String?, rangeKey: String?): Publisher<DynamoDBEntity?>?

    // tag::sample-query-class[]
    class EqRangeProjection : QueryFunction<DynamoDBEntity>({ args: Map<String, Any> -> // <2>
        partitionKey(args["hashKey"]) // <3>
        index(DynamoDBEntity.RANGE_INDEX)
        sortKey { eq(args["rangeKey"]) } // <4>
        only(DynamoDBEntity.RANGE_INDEX) // <5>
    })

    // end::sample-query-class[]
    // tag::sample-query[]
    @Query(EqRangeProjection::class)                                                    // <6>
    fun queryByRangeIndex(hashKey: String, rangeKey: String): Publisher<DynamoDBEntity> // <7>

    // end::sample-query[]
    @Query(BetweenDateIndex::class)
    fun queryByDates(hashKey: String?, after: Date?, before: Date?): List<DynamoDBEntity?>?

    class BetweenDateIndexScroll : QueryFunction<DynamoDBEntity>({ args: Map<String, Any> ->
        index(DynamoDBEntity.DATE_INDEX)
        partitionKey(args["hashKey"])
        lastEvaluatedKey(args["lastEvaluatedKey"])
        sortKey { between(args["after"], args["before"]) }
    })

    @Query(BetweenDateIndexScroll::class)
    fun queryByDatesScroll(
        hashKey: String?,
        after: Date?,
        before: Date?,
        lastEvaluatedKey: DynamoDBEntity?
    ): List<DynamoDBEntity?>?

    fun delete(entity: DynamoDBEntity?)
    fun delete(hashKey: String?, rangeKey: String?)

    @Query(EqRangeIndex::class)
    fun deleteByRangeIndex(hashKey: String?, rangeKey: String?): Int

    @Query(BetweenDateIndex::class)
    fun deleteByDates(hashKey: String?, after: Date?, before: Date?): Int

    // tag::sample-update-class[]
    class IncrementNumber : UpdateFunction<DynamoDBEntity, Int>({ args: Map<String, Any> ->// <2>
        partitionKey(args["hashKey"])                                                   // <3>
        sortKey(args["rangeKey"])                                                       // <4>
        add("number", 1)                                                                // <5>
        returnUpdatedNew(DynamoDBEntity::number)                                        // <6>
    })

    // end::sample-update-class[]
    // tag::sample-update[]
    @Update(IncrementNumber::class)                                                     // <7>
    fun increment(hashKey: String, rangeKey: String): Number                            // <8>

    // end::sample-update[]
    class DecrementNumber : UpdateFunction<DynamoDBEntity, Int>({ args: Map<String, Any> ->
        partitionKey(args["hashKey"])
        sortKey(args["rangeKey"])
        add("number", -1)
        returnUpdatedNew(DynamoDBEntity::number)
    })

    @Update(DecrementNumber::class)
    fun decrement(hashKey: String?, rangeKey: String?): Number?

    // tag::sample-scan-class[]
    class EqRangeScan : ScanFunction<DynamoDBEntity>({ args: Map<String, Any> ->        // <2>
        filter {
            eq(DynamoDBEntity.RANGE_INDEX, args["foo"])                                 // <3>
        }

    })

    // end::sample-scan-class[]
    // tag::sample-scan[]
    @Scan(EqRangeScan::class)                                                           // <4>
    fun scanAllByRangeIndex(foo: String): Publisher<DynamoDBEntity>                     // <5>
    // end::sample-scan[]
    // tag::footer[]
}
