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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class AdvancedQueryOnDeclarativeServiceTest {

    private static final Instant REFERENCE_DATE = Instant.ofEpochMilli(1358487600000L);

    @Inject DynamoDBEntityService s;

    @Test
    public void testJavaService() {
        assertNotNull(s.save(createEntity("1", "1", "foo", 1, Date.from(REFERENCE_DATE))));
        assertNotNull(s.save(createEntity("1", "2", "bar", 1, Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS)))));
        assertNotNull(s.save(createEntity("1", "3", "foo", 2, Date.from(REFERENCE_DATE))));
        assertNotNull(s.save(createEntity("1", "4", "bar", 2, Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS)))));
        assertNotNull(s.save(createEntity("1", "5", "foo", 3, Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS)))));
        assertNotNull(s.save(createEntity("1", "6", "foo", null, Date.from(REFERENCE_DATE.plus(3, ChronoUnit.DAYS)))));
        assertNotNull(s.save(createEntity("2", "1", "bar", 3, Date.from(REFERENCE_DATE))));

        assertThrowsExactly(IllegalArgumentException.class, () -> s.get(null, "1"));

        assertEquals(2, s.countAllByNumber("1", 1));
        assertEquals(1, s.countAllByNumber("1", null));
        assertEquals(6, s.countAllByOptionalNumber("1", null));

        List<DynamoDBEntity> allByNumber = s.findAllByNumber("1", 1);
        assertEquals(2, allByNumber.size());

        DynamoDBEntity first = allByNumber.get(0);
        assertEquals("1", first.getParentId());
        assertEquals("2", first.getId());
        assertEquals("bar", first.getRangeIndex());
        assertEquals(1, first.getNumber());

        assertEquals(3, s.findAllByNumberGreaterThan("1", 1).size());
        assertEquals(5, s.findAllByNumberGreaterThanEqual("1", 1).size());
        assertEquals(2, s.findAllByNumberLowerThan("1", 2).size());
        assertEquals(4, s.findAllByNumberLowerThanEqual("1", 2).size());
        assertEquals(5, s.findAllByNumberNot("1", 3).size());
        assertEquals(5, s.findAllByNumberIsType("1", Number.class).size());
        assertEquals(4, s.findAllByNumberIn("1", List.of(1, 2)).size());
        assertEquals(4, s.findAllByNumberInArray("1", 1, 2).size());
        assertEquals(4, s.findAllByNumberInExplicit("1", List.of(1, 2)).size());
        assertEquals(4, s.findAllByNumberBetween("1", 1, 2).size());
        assertEquals(4, s.findAllByRangeBeginsWith("1", "f").size());

        List<DynamoDBEntity> allByNumberNotPaginated = s.findAllByNumberNot("1", 3, createLastEvaluatedKey("1", "2"), 1, 2);
        assertEquals(2, allByNumberNotPaginated.size());
        assertEquals("3", allByNumberNotPaginated.get(0).getId());
        assertEquals("4", allByNumberNotPaginated.get(1).getId());

        assertEquals(4, s.deleteAllByRangeBeginsWith("1", "f"));
        assertEquals(0, s.findAllByRangeBeginsWith("1", "f").size());

        List<String> ids = IntStream.range(10_000, 11_000).mapToObj(String::valueOf).collect(Collectors.toList());

        s.saveAll(ids.stream().map(id -> createEntity("10000", id, "foo", 1, Date.from(REFERENCE_DATE))).collect(Collectors.toList()));

        assertEquals(ids, s.getAll("10000", ids).stream().map(DynamoDBEntity::getId).collect(Collectors.toList()));
    }

    private DynamoDBEntity createLastEvaluatedKey(String parentId, String id) {
        DynamoDBEntity entity = new DynamoDBEntity();
        entity.setParentId(parentId);
        entity.setId(id);
        return entity;
    }


    private DynamoDBEntity createEntity(String parentId, String id, String rangeIndex, Integer number, Date date) {
        DynamoDBEntity entity = new DynamoDBEntity();
        entity.setParentId(parentId);
        entity.setId(id);
        entity.setRangeIndex(rangeIndex);
        entity.setDate(date);
        entity.setNumber(number);
        return entity;
    }

}
