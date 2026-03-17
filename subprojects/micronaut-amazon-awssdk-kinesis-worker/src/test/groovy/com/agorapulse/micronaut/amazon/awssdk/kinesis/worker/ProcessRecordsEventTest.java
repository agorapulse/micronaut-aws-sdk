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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessRecordsEventTest {

    @Test
    void eventShouldContainAllProperties() {
        // given
        String stream = "test-stream";
        String shardId = "shard-001";
        Long millisBehindLatest = 5000L;
        int recordCount = 10;

        // when
        ProcessRecordsEvent event = new ProcessRecordsEvent(stream, shardId, millisBehindLatest, recordCount);

        // then
        assertEquals(stream, event.getStream());
        assertEquals(shardId, event.getShardId());
        assertEquals(millisBehindLatest, event.getMillisBehindLatest());
        assertEquals(recordCount, event.getRecordCount());
    }

    @Test
    void eventShouldAllowNullMillisBehindLatest() {
        // given
        ProcessRecordsEvent event = new ProcessRecordsEvent("stream", "shard", null, 5);

        // then
        assertNull(event.getMillisBehindLatest());
    }

    @Test
    void toStringShouldContainAllProperties() {
        // given
        ProcessRecordsEvent event = new ProcessRecordsEvent("my-stream", "shard-123", 1000L, 50);

        // when
        String str = event.toString();

        // then
        assertTrue(str.contains("my-stream"));
        assertTrue(str.contains("shard-123"));
        assertTrue(str.contains("1000"));
        assertTrue(str.contains("50"));
    }

}
