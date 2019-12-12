/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisListener;
import com.amazonaws.services.kinesis.model.Record;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// tag::all[]
@Singleton                                                                              // <1>
public class KinesisListenerTester {

    @KinesisListener
    public void listenString(String string) {                                           // <2>
        String message = "EXECUTED: listenString(" + string + ")";
        logExecution(message);
    }

    @KinesisListener
    public void listenRecord(Record record) {                                           // <3>
        logExecution("EXECUTED: listenRecord(" + record + ")");
    }


    @KinesisListener
    public void listenStringRecord(String string, Record record) {                      // <4>
        logExecution("EXECUTED: listenStringRecord(" + string + ", " + record + ")");
    }

    @KinesisListener
    public void listenObject(MyEvent event) {                                           // <5>
        logExecution("EXECUTED: listenObject(" + event + ")");
    }

    @KinesisListener
    public void listenObjectRecord(MyEvent event, Record record) {                      // <6>
        logExecution("EXECUTED: listenObjectRecord(" + event + ", " + record + ")");
    }

    @KinesisListener
    public void listenPogoRecord(Pogo event) {                                          // <7>
        logExecution("EXECUTED: listenPogoRecord(" + event + ")");
    }

    public List<String> getExecutions() {
        return executions;
    }

    public void setExecutions(List<String> executions) {
        this.executions = executions;
    }

    private void logExecution(String message) {
        executions.add(message);
        System.err.println(message);
    }

    private List<String> executions = new CopyOnWriteArrayList<>();
}
// end::all[]
