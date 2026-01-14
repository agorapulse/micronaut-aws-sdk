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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.convert;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.Options;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.ConvertedJson;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.PartitionKey;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class ConvertedJsonEntityExample {

    private String id;
    private Options options;

    public ConvertedJsonEntityExample() {}

    public ConvertedJsonEntityExample(String id, Options options) {
        this.id = id;
        this.options = options;
    }

    @PartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ConvertedJson
    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
