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
package com.agorapulse.micronaut.aws.dynamodb;


import io.micronaut.test.annotation.MicronautTest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class DeclarativeServiceTest {

    private static final DateTime REFERENCE_DATE = new DateTime(1358487600000L);

    @Inject DynamoDBEntityService s;

    @Test
    public void testJavaService() {
        assertNotNull(s.save(createEntity("1", "1", "foo", REFERENCE_DATE.toDate())));
        assertNotNull(s.save(createEntity("1", "2", "bar", REFERENCE_DATE.plusDays(1).toDate())));
        assertNotNull(s.saveAll(Arrays.asList(
            createEntity("2", "1", "foo", REFERENCE_DATE.minusDays(5).toDate()),
            createEntity("2", "2", "foo", REFERENCE_DATE.minusDays(2).toDate())
        )));
        assertNotNull(s.saveAll(
            createEntity("3", "1", "foo", REFERENCE_DATE.plusDays(7).toDate()),
            createEntity("3", "2", "bar", REFERENCE_DATE.plusDays(14).toDate())
        ));

        assertNotNull(s.get("1", "1"));
        assertNotNull(s.load("1", "1"));


        assertEquals(2, s.getAll("1", Arrays.asList("2", "1")).size());
        assertEquals(2, s.loadAll("1", Arrays.asList("2", "1")).size());
        assertEquals(2, s.getAll("1", "2", "1").size());
        assertEquals(0, s.loadAll("1", "3", "4").size());

        assertEquals(2, s.count("1"));
        assertEquals(1, s.count("1", "1"));
        assertEquals(1, s.countByRangeIndex("1", "bar"));
        assertEquals(2, s.countByDates("1", REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()));
        assertEquals(1, s.countByDates("3", REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()));

        assertEquals(2, Flux.from(s.query("1")).count().block().intValue());
        assertEquals(1, Flux.from(s.query("1", "1")).count().block().intValue());
        assertEquals(1, Flux.from(s.queryByRangeIndex("1", "bar")).count().block().intValue());
        assertNull(Flux.from(s.queryByRangeIndex("1", "bar")).blockFirst().parentId);
        assertEquals("bar", Flux.from(s.queryByRangeIndex("1", "bar")).blockFirst().rangeIndex);
        assertEquals(2, s.queryByDates("1", REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()).count().blockingGet());
        assertEquals(1, s.queryByDates("3", REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()).count().blockingGet());

        assertEquals(2, Flux.from(s.scanAllByRangeIndex("bar")).count().block().intValue());

        s.increment("1", "1");
        s.increment("1", "1");
        s.increment("1", "1");
        assertEquals(2, s.decrement("1", "1"));
        assertEquals(2, s.get("1", "1").getNumber().intValue());

        s.delete(s.get("1", "1"));
        assertEquals(0, s.count("1", "1"));
        s.delete("3", "1");
        assertEquals(0, s.count("3", "1"));
        assertEquals(1, s.deleteByRangeIndex("1", "bar"));
        assertEquals(0, s.countByRangeIndex("1", "bar"));
        assertEquals(2, s.deleteByDates("2",  REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()));
        assertEquals(0, s.countByDates("2", REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()));
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
