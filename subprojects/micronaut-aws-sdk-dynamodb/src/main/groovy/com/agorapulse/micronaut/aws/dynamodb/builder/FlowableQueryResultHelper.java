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
package com.agorapulse.micronaut.aws.dynamodb.builder;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;

import java.util.List;

class FlowableQueryResultHelper {

    public static <T> Flowable<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBQueryExpression<T> queryExpression) {
        return Flowable.generate(() -> mapper.queryPage(type, queryExpression), new BiFunction<QueryResultPage<T>, Emitter<List<T>>, QueryResultPage<T>>() {
            @Override
            public QueryResultPage<T> apply(QueryResultPage<T> result, Emitter<List<T>> emitter) throws Exception {
                emitter.onNext(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.onComplete();
                    return null;
                }

                return mapper.queryPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }
        }).flatMap(Flowable::fromIterable);
    }

    public static <T> Flowable<T> generate(Class<T> type, IDynamoDBMapper mapper, DynamoDBScanExpression queryExpression) {
        return Flowable.generate(() -> mapper.scanPage(type, queryExpression), new BiFunction<ScanResultPage<T>, Emitter<List<T>>, ScanResultPage<T>>() {
            @Override
            public ScanResultPage<T> apply(ScanResultPage<T> result, Emitter<List<T>> emitter) throws Exception {
                emitter.onNext(result.getResults());

                if (result.getLastEvaluatedKey() == null) {
                    emitter.onComplete();
                    return null;
                }

                return mapper.scanPage(type, queryExpression.withExclusiveStartKey(result.getLastEvaluatedKey()));
            }
        }).flatMap(Flowable::fromIterable);
    }

}
