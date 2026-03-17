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

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test collector for ProcessRecordsEvent.
 * Demonstrates how users can subscribe to the event for custom metrics publishing.
 */
@Singleton
public class ProcessRecordsEventCollector {

    private final List<ProcessRecordsEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    void onProcessRecords(ProcessRecordsEvent event) {
        events.add(event);
        System.err.println("Received ProcessRecordsEvent: stream=" + event.getStream() 
            + ", shard=" + event.getShardId() 
            + ", millisBehindLatest=" + event.getMillisBehindLatest()
            + ", recordCount=" + event.getRecordCount());
    }

    public List<ProcessRecordsEvent> getEvents() {
        return events;
    }

    public boolean hasReceivedEvents() {
        return !events.isEmpty();
    }

    public void clear() {
        events.clear();
    }

}
