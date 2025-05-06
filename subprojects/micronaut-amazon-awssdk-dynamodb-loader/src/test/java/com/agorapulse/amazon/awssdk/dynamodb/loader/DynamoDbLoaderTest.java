/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
package com.agorapulse.amazon.awssdk.dynamodb.loader;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBServiceProvider;
import com.agorapulse.testing.fixt.Fixt;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest                                                                          // <1>
@Property(name = "aws.dynamodb.create-tables", value = "true")                          // <2>
class DynamoDbLoaderTest {

    private static final Fixt FIXT = Fixt.create(DynamoDbLoaderTest.class);             // <3>

    @Inject DynamoDbLoader loader;
    @Inject DynamoDBServiceProvider provider;

    @Test
    void loadIntoDynamoDb() {

        Map<Class<?>, Iterable<String>> mappings = Map.of(
            TestEntity.class, List.of("test-entity.csv")                                // <4>
        );

        loader.loadAll(FIXT::readText, mappings);                                       // <5>

        TestEntity fromDb = provider.findOrCreate(TestEntity.class).get("1", null);     // <6>
        TestEntity referenceEntity = getReferenceEntity();
        assertEquals(referenceEntity, fromDb);
    }

    private static TestEntity getReferenceEntity() {
        TestEntity referenceEntity = new TestEntity();
        referenceEntity.setId("1");
        referenceEntity.setName("test-one");
        referenceEntity.setActive(true);
        referenceEntity.setCreated(Instant.parse("2019-01-01T00:00:00Z"));
        referenceEntity.setCount(2);
        referenceEntity.setValue(3.4);
        referenceEntity.setData(Map.of("string", "text"));
        referenceEntity.setTags(Set.of("one", "two", "three"));
        return referenceEntity;
    }

}
