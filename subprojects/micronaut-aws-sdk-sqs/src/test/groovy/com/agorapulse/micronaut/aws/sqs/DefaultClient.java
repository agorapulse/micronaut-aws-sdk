/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.aws.sqs;

import com.agorapulse.micronaut.aws.sqs.annotation.Queue;
import com.agorapulse.micronaut.aws.sqs.annotation.QueueClient;

@QueueClient                                                                            // <1>
interface DefaultClient {

    @Queue(value = "OtherQueue", group = "SomeGroup")
    String sendMessageToQueue(String message);                                          // <2>

    String sendMessage(Pogo message);                                                   // <3>

    String sendMessage(byte[] record);                                                  // <4>

    String sendMessage(String record);                                                  // <5>

    String sendMessage(String record, int delay);                                       // <6>

    String sendMessage(String record, String group);                                    // <7>

    String sendMessage(String record, int delay, String group);                         // <8>

    void deleteMessage(String messageId);                                               // <9>

    String OTHER_QUEUE = "OtherQueue";
}
