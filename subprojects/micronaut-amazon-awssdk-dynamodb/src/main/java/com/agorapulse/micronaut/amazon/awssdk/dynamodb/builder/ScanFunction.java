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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public interface ScanFunction<T> extends Function<Map<String, Object>, DetachedScan> {

    DetachedScan<T> scan(Map<String, Object> args);

    @Override
    default DetachedScan apply(Map<String, Object> stringObjectMap) {
        return scan(stringObjectMap);
    }

    default ScanBuilder<T> builder() {
        return Builders.scan();
    }

}
