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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Collections;
import java.util.List;

class FluxQueryResultHelper {

    public static <T> Flux<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBQueryExpression<T> queryExpression) {
        return Flux.generate(
            () -> mapper.queryPage(type, queryExpression),
            (QueryResultPage<T> result, SynchronousSink<List<T>> emitter) -> {
                emitter.next(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.complete();
                    QueryResultPage<T> empty = new QueryResultPage<>();
                    empty.setResults(Collections.emptyList());
                    return empty;
                }

                return mapper.queryPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }).flatMap(Flux::fromIterable);
    }

    public static <T> Flux<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBScanExpression queryExpression) {
        return Flux.generate(
            () -> mapper.scanPage(type, queryExpression),
            (ScanResultPage<T> result, SynchronousSink<List<T>> emitter) -> {
                emitter.next(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.complete();
                    ScanResultPage<T> empty = new ScanResultPage<>();
                    empty.setResults(Collections.emptyList());
                    return empty;
                }

                return mapper.scanPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
        }).flatMap(Flux::fromIterable);
    }

}
