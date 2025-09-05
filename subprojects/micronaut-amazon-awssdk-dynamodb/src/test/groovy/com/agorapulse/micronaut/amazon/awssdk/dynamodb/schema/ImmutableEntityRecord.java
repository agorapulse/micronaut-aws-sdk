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
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondaryPartitionKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SecondarySortKey;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.SortKey;
import io.micronaut.core.annotation.Introspected;

import java.time.Instant;

@Introspected
public record ImmutableEntityRecord(
    @PartitionKey String accountId,
    @SortKey int subId,
    @SecondaryPartitionKey(indexNames = "customers_by_name") String name,
    @SecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"}) Instant createdDate
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Introspected
    public static class Builder {
        private String accountId;
        private int subId;
        private String name;
        private Instant createdDate;
        
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }
        
        public Builder subId(int subId) {
            this.subId = subId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder createdDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }
        
        public ImmutableEntityRecord build() {
            return new ImmutableEntityRecord(accountId, subId, name, createdDate);
        }
    }
}