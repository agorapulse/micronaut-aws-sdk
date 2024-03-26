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
package com.agorapulse.micronaut.aws.dynamodb;


import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

// tag::header[]
@MicronautTest                                                                          // <1>
public class DynamoDBServiceTest {
// end::header[]

    private static final DateTime REFERENCE_DATE = new DateTime(1358487600000L);
    private static final Instant REFERENCE_INSTANT = Instant.ofEpochMilli(1358487600000L);

    // tag::setup[]
    @Inject DynamoDBServiceProvider provider;                                           // <2>
    // end::setup[]

    @Test
    public void testJavaService() {

        //CHECKSTYLE:OFF
        // tag::obtain-service[]
        DynamoDBService<DynamoDBEntity> s = provider.findOrCreate(DynamoDBEntity.class);// <1>
        // end::obtain-service[]
        //CHECKSTYLE:ON

        // tag::create-table[]
        assertNotNull(
            s.createTable(5L, 5L)                                                       // <2>
        );
        // end::create-table[]
        // tag::save-entity[]
        assertNotNull(
            s.save(createEntity("1", "1", "foo", REFERENCE_DATE.toDate()))              // <3>
        );
        // end::save-entity[]
        assertNotNull(s.save(createEntity("1", "2", "bar", REFERENCE_DATE.plusDays(1).toDate())));
        assertNotNull(s.saveAll(Arrays.asList(
            createEntity("2", "1", "foo", REFERENCE_DATE.minusDays(5).toDate()),
            createEntity("2", "2", "foo", REFERENCE_DATE.minusDays(2).toDate())
        )));
        assertNotNull(s.saveAll(
            createEntity("3", "1", "foo", REFERENCE_DATE.plusDays(7).toDate()),
            createEntity("3", "2", "bar", REFERENCE_DATE.plusDays(14).toDate())
        ));

        // tag::load-entity[]
        assertNotNull(
            s.get("1", "1")                                                             // <4>
        );
        // end::load-entity[]


        assertEquals(2, s.getAll("1", Arrays.asList("2", "1")).size());

        assertEquals(2, s.count("1"));
        assertEquals(1, s.count("1",  "1"));
        assertEquals(1, s.count("1", DynamoDBEntity.RANGE_INDEX, "bar"));
        assertEquals(2,
        s.countByDates(
            "1",
            DynamoDBEntity.DATE_INDEX,
            REFERENCE_DATE.minusDays(1).toDate(),
            REFERENCE_DATE.plusDays(2).toDate()
        )
        );
        assertEquals(1, s.countByDates("3", DynamoDBEntity.DATE_INDEX, REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()));

        assertEquals(1, s.countByDates("3", DynamoDBEntity.DATE_INDEX, REFERENCE_INSTANT.plus(9, ChronoUnit.DAYS), REFERENCE_INSTANT.plus(20, ChronoUnit.DAYS)));

        assertNotNull(
            s.query("1")
        );

        assertEquals(1, s.query("1",  "1").getCount().intValue());
        // tag::query-by-range-index[]
        assertEquals(1,
            s.query("1", DynamoDBEntity.RANGE_INDEX, "bar").getCount().intValue()        // <5>
        );
        // end::query-by-range-index[]
        assertEquals("bar", s.query("1", DynamoDBEntity.RANGE_INDEX, "bar").getResults().get(0).rangeIndex);

        assertEquals(
            2,
            s.queryByDates("1", DynamoDBEntity.DATE_INDEX, REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()).getCount().intValue()
        );

        assertEquals(
            2,
            s.queryByDates("1", DynamoDBEntity.DATE_INDEX, REFERENCE_INSTANT.minus(1, ChronoUnit.DAYS), REFERENCE_INSTANT.plus(2, ChronoUnit.DAYS)).getCount().intValue()
        );

        //CHECKSTYLE:OFF
        // tag::query-by-dates[]
        assertEquals(1,
            s.queryByDates(                                                             // <6>
                "3",
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_DATE.plusDays(9).toDate(),
                REFERENCE_DATE.plusDays(20).toDate()
            ).getCount().intValue()
        );
        // end::query-by-dates[]
        //CHECKSTYLE:ON

        // tag::increment[]
        s.increment("1", "1", "number");                                                // <7>
        // end::increment[]

        s.increment("1", "1", "number");
        s.increment("1", "1", "number");
        assertEquals(2, s.decrement("1", "1", "number").intValue());
        assertEquals(2, s.get("1", "1").getNumber().intValue());

        // tag::delete[]
        s.delete(s.get("1", "1"));                                                      // <8>
        // end::delete[]

        assertEquals(0, s.count("1", "1"));
        s.delete("3", "1");
        assertEquals(0, s.count("3", "1"));
        // tag::delete-all[]
        assertEquals(1,
            s.deleteAll("1", DynamoDBEntity.RANGE_INDEX, "bar")                         // <9>
        );
        // end::delete-all[]
        assertEquals(0, s.count("1", DynamoDBEntity.RANGE_INDEX, "bar"));
    }


    private DynamoDBEntity createEntity(String parentId, String id, String rangeIndex, Date date) {
        DynamoDBEntity entity = new DynamoDBEntity();
        entity.setParentId(parentId);
        entity.setId(id);
        entity.setRangeIndex(rangeIndex);
        entity.setDate(date);
        return entity;
    }
}
