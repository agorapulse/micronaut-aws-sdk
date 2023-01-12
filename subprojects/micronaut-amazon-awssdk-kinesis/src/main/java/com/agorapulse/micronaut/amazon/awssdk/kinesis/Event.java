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
package com.agorapulse.micronaut.amazon.awssdk.kinesis;

import java.util.Date;
import java.util.UUID;

public interface Event {

    default String getPartitionKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Warning: this is going to be replaced by {@link java.time.Instant} soon
     */
    Date getTimestamp();
    String getConsumerFilterKey();
    void setConsumerFilterKey(String key);
}
