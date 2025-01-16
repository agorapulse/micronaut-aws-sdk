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
package com.agorapulse.amazon.awssdk.dynamodb.loader;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.Introspected;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Introspected
public class TestEntity {

    @PartitionKey
    private String id;
    private String name;
    private Instant created;
    private boolean active;
    private int count;
    private double value;
    private Map<String, String> data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestEntity that = (TestEntity) o;
        return active == that.active && count == that.count && Double.compare(value, that.value) == 0 && Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(created, that.created) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, created, active, count, value, data);
    }

    @Override
    public String toString() {
        return "TestEntity{id='%s', name='%s', created=%s, active=%s, count=%d, value=%s, data=%s}".formatted(id, name, created, active, count, value, data);
    }
}
