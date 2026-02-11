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

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reproducer for the issue where delete as the first operation on a DynamoDB service
 * fails with CompletionException wrapping ResourceNotFoundException because the table
 * has not been created yet.
 *
 * With create-tables=false, the provider does not create the table eagerly.
 * The table should be created on-demand by the introduction's catch block when
 * the first operation encounters ResourceNotFoundException. However, the async
 * implementation only catches ResourceNotFoundException directly, not when it is
 * wrapped in CompletionException by the AWS async SDK.
 */
@MicronautTest
@Property(name = "aws.dynamodb.create-tables", value = "false")
public class DeleteFirstOperationTest {

    @Inject
    DynamoDBEntityDeleteFirstService service;

    @Test
    public void deleteAsFirstOperationShouldNotFail() {
        // delete on a non-existent item should trigger table creation and succeed
        assertDoesNotThrow(() -> service.delete("org1", "identity1"));
    }

    @Test
    public void findAllAsFirstOperationShouldNotFail() {
        // findAll on a non-existent table should trigger table creation and not throw
        assertDoesNotThrow(() -> service.findAllByOrganizationUid("org1"));
    }

    @Test
    public void deleteAfterFindAllShouldWork() {
        // reproduces the exact pattern from the failing spec setup method
        var items = service.findAllByOrganizationUid("org1");
        for (var item : items) {
            service.delete(item.getOrganizationUid(), item.getIdentityId());
        }

        // also verify save and get work after the table is created
        DynamoDBEntityDeleteFirst entity = new DynamoDBEntityDeleteFirst();
        entity.setOrganizationUid("org1");
        entity.setIdentityId("identity1");
        service.save(entity);

        DynamoDBEntityDeleteFirst saved = service.get("org1", "identity1");
        assertNotNull(saved);
        assertEquals("org1", saved.getOrganizationUid());
        assertEquals("identity1", saved.getIdentityId());

        // cleanup
        service.delete("org1", "identity1");
    }

}
