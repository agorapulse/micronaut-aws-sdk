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
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Guards the cached queue-URL lookup against the reload race that dropped inbox item-update
 * messages: loadQueues() used to clear() then putAll(), so a concurrent reader saw an empty map
 * and reported an existing queue as missing.
 */
class SqsQueueUrlCacheReloadTest {

    private static final String QUEUE = "report_ReportPostInsightUpdate";
    private static final String QUEUE_URL = "https://sqs.eu-west-1.amazonaws.com/123456789012/" + QUEUE;

    private static final int READER_THREADS = 4;
    private static final int RELOADS = 500;

    @Test
    void concurrentReloadNeverHidesAnExistingQueue() throws Exception {
        DefaultSimpleQueueServiceConfiguration configuration = new DefaultSimpleQueueServiceConfiguration();
        configuration.setCache(true);
        SimpleQueueService service = new DefaultSimpleQueueService(listingClient(), configuration);

        service.getQueueUrl(QUEUE);

        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> readers = IntStream.range(0, READER_THREADS)
                .<Future<?>>mapToObj(ignored -> executor.submit(() -> {
                    awaitStart(start);
                    try {
                        for (int i = 0; i < RELOADS && failure.get() == null; i++) {
                            assertEquals(QUEUE_URL, service.getQueueUrl(QUEUE));
                        }
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }))
                .toList();

            Future<?> reloader = executor.submit(() -> {
                awaitStart(start);
                for (int i = 0; i < RELOADS; i++) {
                    service.listQueueNames(true);
                }
            });

            start.countDown();
            for (Future<?> reader : readers) {
                reader.get();
            }
            reloader.get();
        }

        assertNull(failure.get(), () -> "a concurrent reload hid an existing queue: " + failure.get());
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static SqsClient listingClient() {
        ListQueuesResponse response = ListQueuesResponse.builder().queueUrls(QUEUE_URL).build();
        return (SqsClient) Proxy.newProxyInstance(
            SqsClient.class.getClassLoader(),
            new Class<?>[]{SqsClient.class},
            (proxy, method, args) -> {
                if ("listQueues".equals(method.getName())) {
                    return response;
                }
                if ("close".equals(method.getName()) || "serviceName".equals(method.getName())) {
                    return method.getReturnType() == String.class ? "sqs" : null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

}
