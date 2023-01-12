/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.WorkerStateChangeListener;
import io.micronaut.context.event.ApplicationEventListener;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the current state for worker for given stream.
 */
@Singleton
public class WorkerStateListener implements ApplicationEventListener<WorkerStateEvent> {

    private final Map<String, WorkerStateChangeListener.WorkerState> states = new ConcurrentHashMap<>();

    /**
     * Return the current state of the worker.
     * @param stream name of the stream
     * @return the state of the worker or {@link WorkerStateChangeListener.WorkerState#SHUT_DOWN} if unknown
     */
    public WorkerStateChangeListener.WorkerState getState(String stream) {
        return states.getOrDefault(stream, WorkerStateChangeListener.WorkerState.SHUT_DOWN);
    }

    /**
     * Return true if the worker for given stream is ready.
     * @param stream name of the stream
     * @return true if the worker is ready to receive events
     */
    public boolean isReady(String stream) {
        return WorkerStateChangeListener.WorkerState.STARTED.equals(getState(stream));
    }

    @Override
    public void onApplicationEvent(WorkerStateEvent event) {
        states.put(event.getStream(), event.getState());
    }
}
