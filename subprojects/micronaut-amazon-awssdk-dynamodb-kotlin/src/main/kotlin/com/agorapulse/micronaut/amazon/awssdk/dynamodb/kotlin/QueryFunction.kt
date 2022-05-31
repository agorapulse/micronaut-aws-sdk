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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.kotlin

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.DetachedQuery
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.QueryFunction

open class QueryFunction<T>(private val body: (@DynamoDbDsl QueryBuilder<T>).(Map<String, Any>) -> QueryBuilder<T>) : QueryFunction<T> {
    override fun query(args: MutableMap<String, Any>): DetachedQuery<T> {
        return Builders.query { body(QueryBuilder(it), args) }
    }
}
