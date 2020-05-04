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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.*;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.*;
import io.reactivex.Flowable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

// tag::all[]
// tag::header[]
@Service(DynamoDBEntity.class)                                                          // <1>
public interface DynamoDBEntityService {
// end::header[]

    DynamoDBEntity get(@PartitionKey String parentId, @SortKey String id);

    DynamoDBEntity load(@HashKey String parentId, @RangeKey String id);

    List<DynamoDBEntity> getAll(String hash, List<String> rangeKeys);

    List<DynamoDBEntity> getAll(String hash, String... rangeKeys);

    List<DynamoDBEntity> loadAll(String hash, List<String> rangeKeys);

    List<DynamoDBEntity> loadAll(String hash, String... rangeKeys);

    DynamoDBEntity save(DynamoDBEntity entity);

    List<DynamoDBEntity> saveAll(DynamoDBEntity... entities);

    List<DynamoDBEntity> saveAll(Iterable<DynamoDBEntity> entities);

    int count(String hashKey);

    int count(String hashKey, String rangeKey);

    class EqRangeIndex implements QueryFunction<DynamoDBEntity> {

        @Override
        public Consumer<QueryBuilder<DynamoDBEntity>> query(Map<String, Object> arguments) {
            return b -> b.partitionKey(arguments.get("hashKey"))
                .index(DynamoDBEntity.RANGE_INDEX)
                .sortKey(r -> r.eq(arguments.get("rangeKey")));
        }

    }

    @Query(EqRangeIndex.class)
    int countByRangeIndex(String hashKey, String rangeKey);

    class BetweenDateIndex implements Function<Map<String, Object>, DetachedQuery> {
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntity.class)
                .index(DynamoDBEntity.DATE_INDEX)
                .hash(arguments.get("hashKey"))
                .page(1)
                .range(r -> r.between(arguments.get("after"), arguments.get("before")));
        }
    }
    @Query(BetweenDateIndex.class)
    int countByDates(String hashKey, Date after, Date before);

    Flowable<DynamoDBEntity> query(String hashKey);

    Flowable<DynamoDBEntity> query(String hashKey, String rangeKey);

    // tag::sample-query-class[]
    class EqRangeProjection implements QueryFunction<DynamoDBEntity> {                  // <2>

        public Consumer<QueryBuilder<DynamoDBEntity>> query(Map<String, Object> arguments) {
            return b -> b.partitionKey(arguments.get("hashKey"))
                .index(DynamoDBEntity.RANGE_INDEX)
                .sortKey(r ->
                    r.eq(arguments.get("rangeKey"))                                     // <4>
                )
                .only(DynamoDBEntity.RANGE_INDEX);                                      // <5>
        }
    }
    // end::sample-query-class[]
    // tag::sample-query[]
    @Query(EqRangeProjection.class)                                                     // <6>
    Flowable<DynamoDBEntity> queryByRangeIndex(String hashKey, String rangeKey);        // <7>
    // end::sample-query[]

    @Query(BetweenDateIndex.class)
    List<DynamoDBEntity> queryByDates(String hashKey, Date after, Date before);

    class BetweenDateIndexScroll implements Function<Map<String, Object>, DetachedQuery> {
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntity.class)
                .index(DynamoDBEntity.DATE_INDEX)
                .hash(arguments.get("hashKey"))
                .lastEvaluatedKey(arguments.get("lastEvaluatedKey"))
                .range(r -> r.between(arguments.get("after"), arguments.get("before")));
        }
    }
    @Query(BetweenDateIndexScroll.class)
    List<DynamoDBEntity> queryByDatesScroll(String hashKey, Date after, Date before, DynamoDBEntity lastEvaluatedKey);

    void delete(DynamoDBEntity entity);

    void delete(String hashKey, String rangeKey);

    @Query(EqRangeIndex.class)
    int deleteByRangeIndex(String hashKey, String rangeKey);

    @Query(BetweenDateIndex.class)
    int deleteByDates(String hashKey, Date after, Date before);

    // tag::sample-update-class[]
    class IncrementNumber implements UpdateFunction<DynamoDBEntity> {                   // <2>

        @Override
        public Consumer<UpdateBuilder<DynamoDBEntity>> update(Map<String, Object> args) {
            return b -> b.partitionKey(args.get("hashKey"))                             // <3>
                .sortKey(args.get("rangeKey"))                                          // <4>
                .add("number", 1)                                                       // <6>
                .returnUpdatedNew(DynamoDBEntity::getNumber);                           // <6>
        }

    }
    // end::sample-update-class[]
    // tag::sample-update[]
    @Update(IncrementNumber.class)                                                      // <7>
    Number increment(String hashKey, String rangeKey);                                  // <8>
    // end::sample-update[]

    class DecrementNumber implements Function<Map<String, Object>, DetachedUpdate> {
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntity.class)
                .hash(arguments.get("hashKey"))
                .range(arguments.get("rangeKey"))
                .add("number", -1)
                .returnUpdatedNew(DynamoDBEntity::getNumber);
        }
    }

    @Update(DecrementNumber.class)
    Number decrement(String hashKey, String rangeKey);

    // tag::sample-scan-class[]
    class EqRangeScan implements ScanFunction<DynamoDBEntity> {                         // <2>

        @Override
        public Consumer<ScanBuilder<DynamoDBEntity>> scan(Map<String, Object>  args) {
            return b -> b.filter(f ->
                f.eq(DynamoDBEntity.RANGE_INDEX, args.get("foo"))                       // <3>
            );
        }

    }
    // end::sample-scan-class[]
    // tag::sample-scan[]
    @Scan(EqRangeScan.class)                                                            // <4>
    Flowable<DynamoDBEntity> scanAllByRangeIndex(String foo);                           // <5>
    // end::sample-scan[]

// tag::footer[]
}
// end::footer[]
// end::all[]
