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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the cached queue-URL map against two reload hazards that dropped inbox item-update messages:
 * a reader seeing the map mid-reload, and a concurrent createQueue() being discarded by the reload swap.
 */
class SqsQueueUrlCacheReloadTest {

    private static final String BASE = "https://sqs.eu-west-1.amazonaws.com/123456789012/";
    private static final String EXISTING = "report_ReportPostInsightUpdate";
    private static final String CREATED = "report_ReportPostInsightUpdateCreated";

    @Test
    void concurrentReloadKeepsExistingQueueVisibleAndDoesNotDiscardCreatedQueue() throws Exception {
        FakeSqs sqs = new FakeSqs();
        sqs.add(EXISTING);
        DefaultSimpleQueueServiceConfiguration configuration = new DefaultSimpleQueueServiceConfiguration();
        configuration.setCache(true);
        SimpleQueueService service = new DefaultSimpleQueueService(sqs.client(), configuration);

        service.getQueueUrl(EXISTING);

        CountDownLatch reloadInFlight = new CountDownLatch(1);
        CountDownLatch releaseReload = new CountDownLatch(1);
        sqs.blockNextListQueues(reloadInFlight, releaseReload);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> reloader = executor.submit(() -> service.listQueueNames(true));

            reloadInFlight.await();
            // while the reload holds its snapshot, an existing queue must never look missing
            assertEquals(BASE + EXISTING, service.getQueueUrl(EXISTING));
            // ...and a queue created during the reload must survive the swap that follows
            Future<?> creator = executor.submit(() -> service.createQueue(CREATED));

            releaseReload.countDown();
            reloader.get();
            creator.get();
        }

        assertEquals(BASE + EXISTING, service.getQueueUrl(EXISTING));
        assertEquals(BASE + CREATED, service.getQueueUrl(CREATED));
    }

    private static final class FakeSqs {

        private final Set<String> names = ConcurrentHashMap.newKeySet();
        private volatile CountDownLatch listQueuesEntered;
        private volatile CountDownLatch listQueuesReleased;

        void add(String name) {
            names.add(name);
        }

        void blockNextListQueues(CountDownLatch entered, CountDownLatch released) {
            this.listQueuesEntered = entered;
            this.listQueuesReleased = released;
        }

        SqsClient client() {
            return (SqsClient) Proxy.newProxyInstance(
                SqsClient.class.getClassLoader(),
                new Class<?>[]{SqsClient.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "listQueues": {
                            CountDownLatch entered = listQueuesEntered;
                            CountDownLatch released = listQueuesReleased;
                            if (entered != null) {
                                listQueuesEntered = null;
                                listQueuesReleased = null;
                                entered.countDown();
                                released.await();
                            }
                            return ListQueuesResponse.builder()
                                .queueUrls(names.stream().map(name -> BASE + name).toList())
                                .build();
                        }
                        case "createQueue": {
                            String name = ((CreateQueueRequest) args[0]).queueName();
                            names.add(name);
                            return CreateQueueResponse.builder().queueUrl(BASE + name).build();
                        }
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        }

    }

}
