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

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// tag::header[]
@MicronautTest // <1>
class DeclarativeServiceTest {

    @Inject
    lateinit var s: DynamoDBEntityService

    @Test
    fun testJavaService() {
        Assertions.assertNotNull(s.save(createEntity("1", "1", "foo", Date.from(REFERENCE_DATE))))
        Assertions.assertNotNull(
            s.save(
                createEntity(
                    "1",
                    "2",
                    "bar",
                    Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS))
                )
            )
        )
        Assertions.assertNotNull(
            s.saveAll(
                Arrays.asList(
                    createEntity("2", "1", "foo", Date.from(REFERENCE_DATE.minus(5, ChronoUnit.DAYS))),
                    createEntity("2", "2", "foo", Date.from(REFERENCE_DATE.minus(2, ChronoUnit.DAYS)))
                )
            )
        )
        Assertions.assertNotNull(
            s.saveAll(
                listOf(
                createEntity("3", "1", "foo", Date.from(REFERENCE_DATE.plus(7, ChronoUnit.DAYS))),
                createEntity("3", "2", "bar", Date.from(REFERENCE_DATE.plus(14, ChronoUnit.DAYS)))
                )
            )
        )
        Assertions.assertNotNull(s.get("1", "1"))
        Assertions.assertNotNull(s.load("1", "1"))
        Assertions.assertEquals(2, s.getAll("1", Arrays.asList("2", "1"))!!.size)
        Assertions.assertEquals(2, s.loadAll("1", Arrays.asList("2", "1"))!!.size)
        Assertions.assertEquals(2, s.getAll("1", listOf("2", "1"))!!.size)
        Assertions.assertEquals(0, s.loadAll("1", listOf("3", "4"))!!.size)
        Assertions.assertEquals(2, s.count("1"))
        Assertions.assertEquals(1, s.count("1", "1"))
        Assertions.assertEquals(1, s.countByRangeIndex("1", "bar"))
        Assertions.assertEquals(
            2, s.countByDates(
                "1", Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(
                    REFERENCE_DATE.plus(2, ChronoUnit.DAYS)
                )
            )
        )
        Assertions.assertEquals(
            1, s.countByDates(
                "3", Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)), Date.from(
                    REFERENCE_DATE.plus(20, ChronoUnit.DAYS)
                )
            )
        )
        Assertions.assertEquals(
            2, Flux.from(
                s.query("1")
            ).count().block().toInt()
        )
        Assertions.assertEquals(
            1, Flux.from(
                s.query("1", "1")
            ).count().block().toInt()
        )
        Assertions.assertEquals(
            1, Flux.from(
                s.queryByRangeIndex("1", "bar")
            ).count().block().toInt()
        )
        Assertions.assertNull(
            Flux.from(
                s.queryByRangeIndex("1", "bar")
            ).blockFirst()!!.parentId
        )
        Assertions.assertEquals(
            "bar", Flux.from(
                s.queryByRangeIndex("1", "bar")
            ).blockFirst()?.rangeIndex
        )
        val byDates = s.queryByDates(
            "1", Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(
                REFERENCE_DATE.plus(2, ChronoUnit.DAYS)
            )
        )
        Assertions.assertEquals(2, byDates!!.size)
        Assertions.assertEquals(
            1, s.queryByDatesScroll(
                "1",
                Date.from(
                    REFERENCE_DATE.minus(
                        1,
                        ChronoUnit.DAYS
                    )
                ),
                Date.from(
                    REFERENCE_DATE.plus(
                        2,
                        ChronoUnit.DAYS
                    )
                ),
                byDates.iterator().next()
            )!!.size
        )
        Assertions.assertEquals(
            1, s.queryByDates(
                "3",
                Date.from(
                    REFERENCE_DATE.plus(
                        9,
                        ChronoUnit.DAYS
                    )
                ),
                Date.from(
                    REFERENCE_DATE.plus(
                        20,
                        ChronoUnit.DAYS
                    )
                )
            )!!.size
        )
        Assertions.assertEquals(
            2, Flux.from(
                s.scanAllByRangeIndex("bar")
            ).count().block().toInt()
        )
        s.increment("1", "1")
        s.increment("1", "1")
        s.increment("1", "1")
        Assertions.assertEquals(2, s.decrement("1", "1"))
        Assertions.assertEquals(2, s.get("1", "1")!!.number)
        s.delete(s.get("1", "1"))
        Assertions.assertEquals(0, s.count("1", "1"))
        s.delete("3", "1")
        Assertions.assertEquals(0, s.count("3", "1"))
        Assertions.assertEquals(1, s.deleteByRangeIndex("1", "bar"))
        Assertions.assertEquals(0, s.countByRangeIndex("1", "bar"))
        Assertions.assertEquals(
            2, s.deleteByDates(
                "2", Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(
                    REFERENCE_DATE.plus(20, ChronoUnit.DAYS)
                )
            )
        )
        Assertions.assertEquals(
            0, s.countByDates(
                "2", Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(
                    REFERENCE_DATE.plus(20, ChronoUnit.DAYS)
                )
            )
        )
    }

    private fun createEntity(parentId: String, id: String, rangeIndex: String, date: Date): DynamoDBEntity {
        val entity = DynamoDBEntity()
        entity.parentId = parentId
        entity.id = id
        entity.rangeIndex = rangeIndex
        entity.date = date
        return entity
    }

    companion object {
        // end::header[]
        private val REFERENCE_DATE = Instant.ofEpochMilli(1358487600000L)
    }
}
