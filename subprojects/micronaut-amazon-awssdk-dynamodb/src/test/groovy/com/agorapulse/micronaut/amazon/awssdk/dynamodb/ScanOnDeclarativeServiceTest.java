/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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

import com.agorapulse.micronaut.amazon.awssdk.core.client.ClientBuilderProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "test.table.name", value = "DynamoDBScanTest")
@Property(name = "aws.dynamodb.client", value = ClientBuilderProvider.AWS_CRT)
@Property(name = "aws.dynamodb.async-client", value = ClientBuilderProvider.AWS_CRT)
public class ScanOnDeclarativeServiceTest {

    private static final Instant REFERENCE_DATE = Instant.ofEpochMilli(1358487600000L);

    @Inject DynamoDBEntityService s;

    @Test
    public void declarativeScanWithoutPartitionKey() {
        s.save(createEntity("1", "1", 1));
        s.save(createEntity("1", "2", 1));
        s.save(createEntity("1", "3", 2));
        s.save(createEntity("2", "1", 2));
        s.save(createEntity("2", "2", 3));
        s.save(createEntity("2", "3", 3));
        s.save(createEntity("2", "4", null));

        // scans span the whole table, across every partition
        assertEquals(7, s.scanAll().size());

        // the @Filter is applied server-side; the null-number entity is not matched
        assertEquals(4, s.scanAllByNumberGreaterThan(1).size());
        assertEquals(2, s.scanAllByNumberGreaterThan(2).size());
        assertEquals(4, s.countScannedByNumberGreaterThan(1));

        // @Limit + @LastEvaluatedKey page through the whole table without overlap
        List<DynamoDBEntity> firstPage = s.scanAllByNumberGreaterThan(1, null, 2);
        assertEquals(2, firstPage.size());

        List<DynamoDBEntity> secondPage = s.scanAllByNumberGreaterThan(1, firstPage.get(firstPage.size() - 1), 2);
        assertEquals(2, secondPage.size());

        Set<String> firstKeys = firstPage.stream().map(ScanOnDeclarativeServiceTest::key).collect(Collectors.toSet());
        assertTrue(secondPage.stream().map(ScanOnDeclarativeServiceTest::key).noneMatch(firstKeys::contains));
    }

    private static String key(DynamoDBEntity entity) {
        return entity.getParentId() + "/" + entity.getId();
    }

    private DynamoDBEntity createEntity(String parentId, String id, Integer number) {
        DynamoDBEntity entity = new DynamoDBEntity();
        entity.setParentId(parentId);
        entity.setId(id);
        entity.setRangeIndex("range-" + id);
        entity.setDate(Date.from(REFERENCE_DATE));
        entity.setNumber(number);
        return entity;
    }

}
