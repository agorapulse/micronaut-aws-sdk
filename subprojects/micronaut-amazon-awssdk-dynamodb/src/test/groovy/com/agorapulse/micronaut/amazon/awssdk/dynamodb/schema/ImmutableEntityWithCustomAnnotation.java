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

import java.time.Instant;
import java.util.Objects;

@Introspected(builder = @Introspected.IntrospectionBuilder(builderClass = ImmutableEntityWithCustomAnnotation.Builder.class))
public class ImmutableEntityWithCustomAnnotation {

    private final Long id;
    private final Long sortKey;
    private final Instant created;

    private ImmutableEntityWithCustomAnnotation(Builder builder) {
        this.id = builder.id;
        this.sortKey = builder.sortKey;
        this.created = builder.created;
    }

    @PartitionKey
    public Long getId() {
        return id;
    }

    @SortKey
    public Long getSortKey() {
        return sortKey;
    }

    @TimeToLive("45d")
    public Instant getCreated() {
        return created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableEntityWithCustomAnnotation that = (ImmutableEntityWithCustomAnnotation) o;
        return Objects.equals(id, that.id) && Objects.equals(sortKey, that.sortKey) && Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sortKey, created);
    }

    @Override
    public String toString() {
        return "ImmutableEntityWithCustomAnnotation{" +
            "id=" + id +
            ", sortKey=" + sortKey +
            ", created=" + created +
            '}';
    }

    public static class Builder {
        private Long id;
        private Long sortKey;
        private Instant created;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sortKey(Long sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        public Builder created(Instant created) {
            this.created = created;
            return this;
        }

        public ImmutableEntityWithCustomAnnotation build() {
            return new ImmutableEntityWithCustomAnnotation(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
