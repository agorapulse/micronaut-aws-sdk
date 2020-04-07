/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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


import io.micronaut.context.ApplicationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.*;

public class DeclarativeServiceTest {

    private static final Instant REFERENCE_DATE = Instant.ofEpochMilli(1358487600000L);

    @Rule
    public LocalStackV2Container localstack = new LocalStackV2Container().withServices(LocalStackV2Container.Service.DYNAMODB);
    public ApplicationContext context;

    @Before
    public void setup() throws URISyntaxException {
        DynamoDbClient client = DynamoDbClient
            .builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackV2Container.Service.DYNAMODB))
            .credentialsProvider(localstack.getDefaultCredentialsProvider())
            .region(Region.EU_WEST_1)
            .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
            .builder()
            .dynamoDbClient(client)
            .build();

        context = ApplicationContext.build().build();
        context.registerSingleton(DynamoDbClient.class, client);
        context.registerSingleton(DynamoDbEnhancedClient.class, enhancedClient);
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

        assertNotNull(s.save(createEntity("1", "1", "foo", Date.from(REFERENCE_DATE))));
        assertNotNull(s.save(createEntity("1", "2", "bar", Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS)))));
        assertNotNull(s.saveAll(Arrays.asList(
            createEntity("2", "1", "foo", Date.from(REFERENCE_DATE.minus(5, ChronoUnit.DAYS))),
            createEntity("2", "2", "foo", Date.from(REFERENCE_DATE.minus(2, ChronoUnit.DAYS)))
        )));
        assertNotNull(s.saveAll(
            createEntity("3", "1", "foo", Date.from(REFERENCE_DATE.plus(7, ChronoUnit.DAYS))),
            createEntity("3", "2", "bar", Date.from(REFERENCE_DATE.plus(14, ChronoUnit.DAYS)))
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
        assertEquals(2, s.countByDates("1", Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS))));
        assertEquals(1, s.countByDates("3", Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))));

        assertEquals(2, s.query("1").count().blockingGet().intValue());
        assertEquals(1, s.query("1", "1").count().blockingGet().intValue());
        assertEquals(1, s.queryByRangeIndex("1", "bar").count().blockingGet().intValue());
        assertNull(s.queryByRangeIndex("1", "bar").blockingSingle().getParentId());
        assertEquals("bar", s.queryByRangeIndex("1", "bar").blockingSingle().getRangeIndex());
        assertEquals(2, s.queryByDates("1", Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS))).size());
        assertEquals(1, s.queryByDates("3", Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))).size());

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
        assertEquals(2, s.deleteByDates("2",  Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))));
        assertEquals(0, s.countByDates("2", Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))));
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
