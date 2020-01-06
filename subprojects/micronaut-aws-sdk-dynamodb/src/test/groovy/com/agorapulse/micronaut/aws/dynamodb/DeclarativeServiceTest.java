/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import io.micronaut.context.ApplicationContext;
import org.joda.time.DateTime;
import org.junit.*;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.*;

public class DeclarativeServiceTest {

    private static final DateTime REFERENCE_DATE = new DateTime(1358487600000L);

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer().withServices(LocalStackContainer.Service.DYNAMODB);
    public ApplicationContext context;

    @Before
    public void setup() {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();

        IDynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);

        context = ApplicationContext.build().build();
        context.registerSingleton(AmazonDynamoDB.class, amazonDynamoDB);
        context.registerSingleton(IDynamoDBMapper.class, mapper);
        context.start();
    }

    @After
    public void cleanup() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testJavaService() {
        DynamoDBEntityService s = context.getBean(DynamoDBEntityService.class);

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

        assertEquals(2, s.query("1").count().blockingGet().intValue());
        assertEquals(1, s.query("1", "1").count().blockingGet().intValue());
        assertEquals(1, s.queryByRangeIndex("1", "bar").count().blockingGet().intValue());
        assertNull(s.queryByRangeIndex("1", "bar").blockingSingle().parentId);
        assertEquals("bar", s.queryByRangeIndex("1", "bar").blockingSingle().rangeIndex);
        assertEquals(2, s.queryByDates("1", REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()).size());
        assertEquals(1, s.queryByDates("3", REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()).size());

        assertEquals(2, s.scanAllByRangeIndex("bar").count().blockingGet().intValue());

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
