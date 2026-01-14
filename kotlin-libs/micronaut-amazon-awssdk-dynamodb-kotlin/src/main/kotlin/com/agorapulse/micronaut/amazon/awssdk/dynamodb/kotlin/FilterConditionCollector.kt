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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

class FilterConditionCollector<T>(private val delegate: com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>) {

    fun inList(attributeOrIndex: String, vararg values: Any?): FilterConditionCollector<T> {
        delegate.inList(attributeOrIndex, values)
        return this
    }

    fun inList(attributeOrIndex: String, values: Collection<*>): FilterConditionCollector<T> {
        delegate.inList(attributeOrIndex, values)
        return this
    }

    fun eq(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.eq(attributeOrIndex, value)
        return this
    }

    fun ne(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.ne(attributeOrIndex, value)
        return this
    }

    fun le(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.le(attributeOrIndex, value)
        return this
    }

    fun lt(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.lt(attributeOrIndex, value)
        return this
    }

    fun ge(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.ge(attributeOrIndex, value)
        return this
    }

    fun gt(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.gt(attributeOrIndex, value)
        return this
    }

    fun sizeEq(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeEq(attributeOrIndex, value)
        return this
    }

    fun sizeNe(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeNe(attributeOrIndex, value)
        return this
    }

    fun sizeLe(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeLe(attributeOrIndex, value)
        return this
    }

    fun sizeLt(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeLt(attributeOrIndex, value)
        return this
    }

    fun sizeGe(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeGe(attributeOrIndex, value)
        return this
    }

    fun sizeGt(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.sizeGt(attributeOrIndex, value)
        return this
    }

    fun between(attributeOrIndex: String, lo: Any?, hi: Any?): FilterConditionCollector<T> {
        delegate.between(attributeOrIndex, lo, hi)
        return this
    }

    fun notExists(attributeOrIndex: String): FilterConditionCollector<T> {
        delegate.notExists(attributeOrIndex)
        return this
    }

    fun isNull(attributeOrIndex: String): FilterConditionCollector<T> {
        delegate.isNull(attributeOrIndex)
        return this
    }

    fun contains(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.contains(attributeOrIndex, value)
        return this
    }

    fun notContains(attributeOrIndex: String, value: Any?): FilterConditionCollector<T> {
        delegate.notContains(attributeOrIndex, value)
        return this
    }

    fun typeOf(attributeOrIndex: String, type: Class<*>): FilterConditionCollector<T> {
        delegate.typeOf(attributeOrIndex, type)
        return this
    }

    fun beginsWith(attributeOrIndex: String, value: String): FilterConditionCollector<T> {
        delegate.beginsWith(attributeOrIndex, value)
        return this
    }


    fun group(conditions: FilterConditionCollector<T>.() -> FilterConditionCollector<T>): FilterConditionCollector<T> {
        delegate.group { conditions(FilterConditionCollector(it)) }
        return this
    }

    fun or(conditions: FilterConditionCollector<T>.() -> FilterConditionCollector<T>): FilterConditionCollector<T> {
        delegate.or { conditions(FilterConditionCollector(it)) }
        return this
    }

    fun and(conditions: FilterConditionCollector<T>.() -> FilterConditionCollector<T>): FilterConditionCollector<T> {
        delegate.and { conditions(FilterConditionCollector(it)) }
        return this
    }

}
