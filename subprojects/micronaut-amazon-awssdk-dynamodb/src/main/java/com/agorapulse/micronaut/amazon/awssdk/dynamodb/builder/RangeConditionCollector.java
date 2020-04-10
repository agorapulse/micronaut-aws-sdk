/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import java.util.function.Consumer;

public interface RangeConditionCollector<T> {

    RangeConditionCollector<T> eq(Object value);

    RangeConditionCollector<T> le(Object value);

    RangeConditionCollector<T> lt(Object value);

    RangeConditionCollector<T> ge(Object value);

    RangeConditionCollector<T> gt(Object value);

    RangeConditionCollector<T> between(Object lo, Object hi);

    RangeConditionCollector<T> beginsWith(String value);

    RangeConditionCollector<T> group(Consumer<RangeConditionCollector<T>> conditions);

    RangeConditionCollector<T> or(Consumer<RangeConditionCollector<T>> conditions);

    RangeConditionCollector<T> and(Consumer<RangeConditionCollector<T>> conditions);

}
