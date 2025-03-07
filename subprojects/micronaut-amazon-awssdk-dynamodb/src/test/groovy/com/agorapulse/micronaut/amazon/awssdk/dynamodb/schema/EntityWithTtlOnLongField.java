/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.schema;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.TimeToLive;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class EntityWithTtlOnLongField {

    @PartitionKey
    private Long id;

    @SortKey
    private Long sortKey;

    @TimeToLive(value = "3d")
    private Long created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSortKey() {
        return sortKey;
    }

    public void setSortKey(Long sortKey) {
        this.sortKey = sortKey;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
