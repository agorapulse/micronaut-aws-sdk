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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder;

import java.util.Collection;
import java.util.function.Consumer;

public interface FilterConditionCollector<T> {

    FilterConditionCollector<T> inList(String attributeOrIndex, Object... values);

    FilterConditionCollector<T> inList(String attributeOrIndex, Collection<?> values);

    FilterConditionCollector<T> eq(String attributeOrIndex, Object value);

    FilterConditionCollector<T> ne(String attributeOrIndex, Object value);

    FilterConditionCollector<T> le(String attributeOrIndex, Object value);

    FilterConditionCollector<T> lt(String attributeOrIndex, Object value);

    FilterConditionCollector<T> ge(String attributeOrIndex, Object value);

    FilterConditionCollector<T> gt(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeEq(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeNe(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeLe(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeLt(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeGe(String attributeOrIndex, Object value);

    FilterConditionCollector<T> sizeGt(String attributeOrIndex, Object value);

    FilterConditionCollector<T> between(String attributeOrIndex, Object lo, Object hi);

    FilterConditionCollector<T> notExists(String attributeOrIndex);

    FilterConditionCollector<T> isNull(String attributeOrIndex);

    FilterConditionCollector<T> contains(String attributeOrIndex, Object value);

    FilterConditionCollector<T> notContains(String attributeOrIndex, Object value);

    FilterConditionCollector<T> typeOf(String attributeOrIndex, Class<?> type);

    FilterConditionCollector<T> beginsWith(String attributeOrIndex, String value);

    FilterConditionCollector<T> group(Consumer<FilterConditionCollector<T>> conditions);

    FilterConditionCollector<T> or(Consumer<FilterConditionCollector<T>> conditions);

    FilterConditionCollector<T> and(Consumer<FilterConditionCollector<T>> conditions);

}
