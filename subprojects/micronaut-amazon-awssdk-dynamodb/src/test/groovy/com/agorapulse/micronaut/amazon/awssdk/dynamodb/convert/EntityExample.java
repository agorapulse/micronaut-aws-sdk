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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.ConvertedBy;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class EntityExample {

    private String id;
    private String data;

    public EntityExample() {}

    public EntityExample(String id, String data) {
        this.id = id;
        this.data = data;
    }

    @PartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ConvertedBy(value = CompressedStringConverter.class)
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
