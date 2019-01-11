package com.agorapulse.micronaut.aws.dynamodb;

import com.agorapulse.micronaut.aws.dynamodb.annotation.Query;
import com.agorapulse.micronaut.aws.dynamodb.annotation.Scan;
import com.agorapulse.micronaut.aws.dynamodb.annotation.Service;
import com.agorapulse.micronaut.aws.dynamodb.annotation.Update;
import com.agorapulse.micronaut.aws.dynamodb.builder.Builders;
import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedQuery;
import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedScan;
import com.agorapulse.micronaut.aws.dynamodb.builder.DetachedUpdate;
import io.reactivex.Flowable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// tag::all[]
// tag::header[]
@Service(DynamoDBEntity.class)                                                          // <1>
public interface DynamoDBEntityService {
// end::header[]

    class EqRangeIndex implements Function<Map<String, Object>, DetachedQuery> {
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntity.class)
                .hash(arguments.get("hashKey"))
                .range(r -> r.eq(DynamoDBEntity.RANGE_INDEX, arguments.get("rangeKey")));
        }
    }

    // tag::sample-query-class[]
    class EqRangeProjection implements Function<Map<String, Object>, DetachedQuery> {   // <2>
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntity.class)                                 // <3>
                .hash(arguments.get("hashKey"))                                         // <4>
                .range(r ->
                    r.eq(DynamoDBEntity.RANGE_INDEX, arguments.get("rangeKey"))         // <5>
                )
                .only(DynamoDBEntity.RANGE_INDEX);                                      // <6>
        }
    }
    // end::sample-query-class[]

    // tag::sample-scan-class[]
    class EqRangeScan implements Function<Map<String, Object>, DetachedScan> {          // <2>
        public DetachedScan apply(Map<String, Object> arguments) {
            return Builders.scan(DynamoDBEntity.class)                                  // <3>
                .filter(f -> f.eq(DynamoDBEntity.RANGE_INDEX, arguments.get("foo")));   // <4>
        }
    }
    // end::sample-scan-class[]

    class BetweenDateIndex implements Function<Map<String, Object>, DetachedQuery> {
        public DetachedQuery apply(Map<String, Object> arguments) {
            return Builders.query(DynamoDBEntity.class)
                .hash(arguments.get("hashKey"))
                .range(r -> r.between(DynamoDBEntity.DATE_INDEX, arguments.get("after"), arguments.get("before")));
        }
    }

    // tag::sample-update-class[]
    class IncrementNumber implements Function<Map<String, Object>, DetachedUpdate> {    // <2>
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntity.class)                                // <3>
                .hash(arguments.get("hashKey"))                                         // <4>
                .range(arguments.get("rangeKey"))                                       // <5>
                .add("number", 1)                                                       // <6>
                .returnUpdatedNew(DynamoDBEntity::getNumber);                           // <7>
        }
    }
    // end::sample-update-class[]

    class DecrementNumber implements Function<Map<String, Object>, DetachedUpdate> {
        public DetachedUpdate apply(Map<String, Object> arguments) {
            return Builders.update(DynamoDBEntity.class)
                .hash(arguments.get("hashKey"))
                .range(arguments.get("rangeKey"))
                .add("number", -1)
                .returnUpdatedNew(DynamoDBEntity::getNumber);
        }
    }

    DynamoDBEntity get(String hash, String rangeKey);

    DynamoDBEntity load(String hash, String rangeKey);

    List<DynamoDBEntity> getAll(String hash, List<String> rangeKeys);

    List<DynamoDBEntity> getAll(String hash, String... rangeKeys);

    List<DynamoDBEntity> loadAll(String hash, List<String> rangeKeys);

    List<DynamoDBEntity> loadAll(String hash, String... rangeKeys);

    DynamoDBEntity save(DynamoDBEntity entity);

    List<DynamoDBEntity> saveAll(DynamoDBEntity... entities);

    List<DynamoDBEntity> saveAll(Iterable<DynamoDBEntity> entities);

    int count(String hashKey);

    int count(String hashKey, String rangeKey);

    @Query(EqRangeIndex.class)
    int countByRangeIndex(String hashKey, String rangeKey);

    @Query(BetweenDateIndex.class)
    int countByDates(String hashKey, Date after, Date before);

    Flowable<DynamoDBEntity> query(String hashKey);

    Flowable<DynamoDBEntity> query(String hashKey, String rangeKey);

    // tag::sample-query[]
    @Query(EqRangeProjection.class)                                                     // <7>
    Flowable<DynamoDBEntity> queryByRangeIndex(String hashKey, String rangeKey);        // <8>
    // end::sample-query[]

    @Query(BetweenDateIndex.class)
    List<DynamoDBEntity> queryByDates(String hashKey, Date after, Date before);

    void delete(DynamoDBEntity entity);

    void delete(String hashKey, String rangeKey);

    @Query(EqRangeIndex.class)
    int deleteByRangeIndex(String hashKey, String rangeKey);

    @Query(BetweenDateIndex.class)
    int deleteByDates(String hashKey, Date after, Date before);

    // tag::sample-update[]
    @Update(IncrementNumber.class)                                                      // <8>
    Number increment(String hashKey, String rangeKey);                                  // <9>
    // end::sample-update[]

    @Update(DecrementNumber.class)
    Number decrement(String hashKey, String rangeKey);

    // tag::sample-scan[]
    @Scan(EqRangeScan.class)                                                            // <5>
    Flowable<DynamoDBEntity> scanAllByRangeIndex(String foo);                           // <6>
    // end::sample-scan[]

// tag::footer[]
}
// end::footer[]
// end::all[]
