/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
import javax.annotation.Nullable;
import org.reactivestreams.Publisher;

import java.util.Date;
import java.util.List;
import java.util.Map;

// tag::all[]
// tag::header[]
@Service(value = DynamoDBEntity.class, tableName = "${test.table.name:DynamoDBJava}")   // <1>
public interface DynamoDBEntityService {
// end::header[]

    DynamoDBEntity get(@PartitionKey String parentId, @SortKey String id);

    DynamoDBEntity load(@PartitionKey String parentId, @SortKey String id);

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
        public DetachedQuery<DynamoDBEntity> query(Map<String, Object> arguments) {
            return builder().partitionKey(arguments.get("hashKey"))
                .index(DynamoDBEntity.RANGE_INDEX)
                .sortKey(r -> r.eq(arguments.get("rangeKey")));
        }

    }

    @Query(EqRangeIndex.class)
    int countByRangeIndex(String hashKey, String rangeKey);

    @Consistent
    @Index(DynamoDBEntity.RANGE_INDEX)
    int countByRangeIndexUsingAnnotation(String hashKey, String rangeKey);

    class BetweenDateIndex implements QueryFunction<DynamoDBEntity> {

        @Override
        public DetachedQuery<DynamoDBEntity> query(Map<String, Object> args) {
            return builder().index(DynamoDBEntity.DATE_INDEX)
                .partitionKey(args.get("hashKey"))
                .page(1)
                .sortKey(r -> r.between(args.get("after"), args.get("before")));
        }

    }
    @Query(BetweenDateIndex.class)
    int countByDates(String hashKey, Date after, Date before);

    Publisher<DynamoDBEntity> query(String hashKey);

    Publisher<DynamoDBEntity> query(String hashKey, String rangeKey);

    // tag::sample-query-class[]
    class EqRangeProjection implements QueryFunction<DynamoDBEntity> {                  // <2>

        public QueryBuilder<DynamoDBEntity> query(Map<String, Object> arguments) {
            return builder().partitionKey(arguments.get("hashKey"))                     // <3>
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
    Publisher<DynamoDBEntity> queryByRangeIndex(String hashKey, String rangeKey);       // <7>
    // end::sample-query[]

    @Query(BetweenDateIndex.class)
    List<DynamoDBEntity> queryByDates(String hashKey, Date after, Date before);

    class BetweenDateIndexScroll implements QueryFunction<DynamoDBEntity> {
        public QueryBuilder<DynamoDBEntity> query(Map<String, Object> arguments) {
            return builder()
                .index(DynamoDBEntity.DATE_INDEX)
                .partitionKey(arguments.get("hashKey"))
                .lastEvaluatedKey(arguments.get("lastEvaluatedKey"))
                .sortKey(r -> r.between(arguments.get("after"), arguments.get("before")));
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
    class IncrementNumber implements UpdateFunction<DynamoDBEntity, Integer> {          // <2>

        @Override
        public UpdateBuilder<DynamoDBEntity, Integer> update(Map<String, Object> args) {
            return builder().partitionKey(args.get("hashKey"))                          // <3>
                .sortKey(args.get("rangeKey"))                                          // <4>
                .add("number", 1)                                                       // <5>
                .returnUpdatedNew(DynamoDBEntity::getNumber);                           // <6>
        }

    }
    // end::sample-update-class[]
    // tag::sample-update[]
    @Update(IncrementNumber.class)                                                      // <7>
    Number increment(String hashKey, String rangeKey);                                  // <8>
    // end::sample-update[]

    class DecrementNumber implements UpdateFunction<DynamoDBEntity, Integer> {
        @Override
        public UpdateBuilder<DynamoDBEntity, Integer> update(Map<String, Object> arguments){
            return builder()
                .partitionKey(arguments.get("hashKey"))
                .sortKey(arguments.get("rangeKey"))
                .add("number", -1)
                .returnUpdatedNew(DynamoDBEntity::getNumber);
        }
    }

    @Update(DecrementNumber.class)
    Number decrement(String hashKey, String rangeKey);

    // tag::sample-scan-class[]
    class EqRangeScan implements ScanFunction<DynamoDBEntity> {                         // <2>

        @Override
        public ScanBuilder<DynamoDBEntity> scan(Map<String, Object>  args) {
            return builder().filter(f ->
                f.eq(DynamoDBEntity.RANGE_INDEX, args.get("foo"))                       // <3>
            );
        }

    }
    // end::sample-scan-class[]
    // tag::sample-scan[]
    @Scan(EqRangeScan.class)                                                            // <4>
    Publisher<DynamoDBEntity> scanAllByRangeIndex(String foo);                          // <5>
    // end::sample-scan[]

    // CHECKSTYLE:OFF
    // tag::advanced-query-methods[]
    @Consistent                                                                         // <1>
    @Descending                                                                         // <2>
    @Index(DynamoDBEntity.DATE_INDEX)                                                   // <3>
    List<DynamoDBEntity> findAllByNumber(
        @PartitionKey String parentId,
        Integer number                                                                  // <4>
    );

    int countAllByOptionalNumber(
        @PartitionKey String parentId,
        @Nullable Integer number                                                        // <5>
    );

    List<DynamoDBEntity> findAllByNumberGreaterThan(
        @PartitionKey String parentId,
        @Filter(                                                                        // <6>
            value = Filter.Operator.GT,
            name = "number"                                                             // <7>
        ) Integer theNumber
    );

    @Index(DynamoDBEntity.RANGE_INDEX)
    List<DynamoDBEntity> findAllByRangeBeginsWith(
        @PartitionKey String parentId,
        @SortKey                                                                        // <8>
        @Filter(
            value = Filter.Operator.BEGINS_WITH,                                        // <9>
            name = "rangeIndex"                                                         // <10>
        )
        String rangeIndexPrefix
    );
    // end::advanced-query-methods[]
    // CHECKSTYLE:ON

    List<DynamoDBEntity> findAllByNumberGreaterThanEqual(
        @PartitionKey String parentId,
        @Filter(Filter.Operator.GE) Integer number
    );

    List<DynamoDBEntity> findAllByNumberLowerThan(
        @PartitionKey String parentId,
        @Filter(Filter.Operator.LT) Integer number
    );

    List<DynamoDBEntity> findAllByNumberLowerThanEqual(
        @PartitionKey String parentId,
        @Filter(Filter.Operator.LE) Integer number
    );

    List<DynamoDBEntity> findAllByNumberNot(
        @PartitionKey String parentId,
        @Filter(Filter.Operator.NE) Integer number
    );

    List<DynamoDBEntity> findAllByNumberIn(
        @PartitionKey String parentId,
        List<Integer> number
    );

    List<DynamoDBEntity> findAllByNumberInArray(
        @PartitionKey String parentId,
        Integer... number
    );

    List<DynamoDBEntity> findAllByNumberInExplicit(
        @PartitionKey String parentId,
        @Filter(Filter.Operator.IN_LIST) List<Integer> number
    );

    List<DynamoDBEntity> findAllByNumberIsType(
        @PartitionKey String parentId,
        @Filter(value = Filter.Operator.TYPE_OF, name = "number") Class<?> type
    );

    List<DynamoDBEntity> findAllByNumberBetween(
        @PartitionKey String parentId,
        @Filter(value = Filter.Operator.BETWEEN, name = "number") Integer numberFrom,
        @Filter(name = "number") Integer numberTo
    );

    @Index(DynamoDBEntity.RANGE_INDEX)
    List<DynamoDBEntity> findAllByRangeContains(
        @PartitionKey String parentId,
        @SortKey @Filter(value = Filter.Operator.CONTAINS, name = "rangeIndex") String string
    );

    @Index(DynamoDBEntity.RANGE_INDEX)
    List<DynamoDBEntity> findAllByRangeNotContains(
        @PartitionKey String parentId,
        @SortKey @Filter(value = Filter.Operator.NOT_CONTAINS, name = "rangeIndex") String string
    );

    int countAllByNumber(@PartitionKey String parentId, Integer number);


// tag::footer[]
}
// end::footer[]
// end::all[]
