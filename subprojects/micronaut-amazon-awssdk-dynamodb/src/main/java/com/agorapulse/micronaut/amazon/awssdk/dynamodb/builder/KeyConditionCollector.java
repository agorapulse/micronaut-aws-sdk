/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
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

public interface KeyConditionCollector<T> {

    KeyConditionCollector<T> eq(Object value);

    KeyConditionCollector<T> le(Object value);

    KeyConditionCollector<T> lt(Object value);

    KeyConditionCollector<T> ge(Object value);

    KeyConditionCollector<T> gt(Object value);

    KeyConditionCollector<T> between(Object lo, Object hi);

    KeyConditionCollector<T> beginsWith(String value);

}
