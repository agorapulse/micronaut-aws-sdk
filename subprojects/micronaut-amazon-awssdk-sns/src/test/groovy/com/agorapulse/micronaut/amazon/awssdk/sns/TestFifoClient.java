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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.MessageDeduplicationId;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.MessageGroupId;
import com.agorapulse.micronaut.amazon.awssdk.sns.annotation.NotificationClient;

@NotificationClient(value = "test", topic = TestFifoClient.TOPIC_NAME) interface TestFifoClient {

    String TOPIC_NAME = "testFifoTopic.fifo";
    String publishFifoMessage(Pogo message, @MessageGroupId String groupId, @MessageDeduplicationId String deduplicationId);
}
