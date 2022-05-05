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
package com.agorapulse.micronaut.aws.dynamodb

import com.agorapulse.micronaut.aws.dynamodb.annotation.Query
import com.agorapulse.micronaut.aws.dynamodb.annotation.Scan
import com.agorapulse.micronaut.aws.dynamodb.annotation.Service
import com.agorapulse.micronaut.aws.dynamodb.annotation.Update
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import io.micronaut.test.annotation.MicronautTest
import io.reactivex.Flowable
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Stepwise

import javax.inject.Inject
import java.time.Instant
import java.time.temporal.ChronoUnit

// tag::builders-import[]
import static com.agorapulse.micronaut.aws.dynamodb.builder.Builders.*                  // <1>

// end::builders-import[]

/**
 * Specification for testing DefaultDynamoDBService using entity with range key.
 */
@SuppressWarnings([
    'NoWildcardImports',
    'UnnecessaryObjectReferences',
])
@Stepwise
// tag::header[]
@MicronautTest                                                                          // <1>
class DefaultDynamoDBServiceSpec extends Specification {

// end::header[]

    private static final DateTime REFERENCE_DATE = new DateTime(1358487600000)
    private static final Instant REFERENCE_INSTANT = Instant.ofEpochMilli(1358487600000)

    @Inject AmazonDynamoDB amazonDynamoDB
    @Inject IDynamoDBMapper mapper
    @Inject DynamoDBItemDBService itemService
    // tag::setup[]
    @Inject DynamoDBServiceProvider provider                                            // <2>

    DynamoDBService<DynamoDBEntity> s

    void setup() {
        s = provider.findOrCreate(DynamoDBEntity)                                       // <3>
    }
    // end::setup[]

    // end::testcontainers-setup[]

    void 'required DynamoDB table annotation'() {
        when:
            new DefaultDynamoDBService<>(amazonDynamoDB, new DynamoDBMapper(amazonDynamoDB), Map)
        then:
            thrown RuntimeException
    }

    void 'read metadata from getters'() {
        when:
            DynamoDBService<DynamoDBEntityWithAnnotationsOnMethods> service =
                new DefaultDynamoDBService<>(amazonDynamoDB, new DynamoDBMapper(amazonDynamoDB), DynamoDBEntityWithAnnotationsOnMethods)
        then:
            service.hashKeyName == 'parentId'
            service.hashKeyClass == String
            service.rangeKeyName == 'id'
            service.rangeKeyClass == String
            service.isIndexRangeKey(DynamoDBEntity.RANGE_INDEX)
            service.isIndexRangeKey(DynamoDBEntity.DATE_INDEX)
    }

    void 'check metadata'() {
        expect:
            s.hashKeyName == 'parentId'
            s.hashKeyClass == String
            s.rangeKeyName == 'id'
            s.rangeKeyClass == String
            s.isIndexRangeKey(DynamoDBEntity.RANGE_INDEX)
            s.isIndexRangeKey(DynamoDBEntity.DATE_INDEX)
            s.newInstance instanceof DynamoDBEntity
    }

    void 'new table'() {
        expect:
        // tag::create-table[]
        s.createTable()                                                                 // <2>
        // end::create-table[]

        when:
            CreateTableResult none = s.createTable()
        then:
            !none
    }

    void 'save items'() {
        when:
        // tag::save-entity[]
        s.save(new DynamoDBEntity(                                                      // <3>
            parentId: '1',
            id: '1',
            rangeIndex: 'foo',
            date: REFERENCE_DATE.toDate()
        ))
        // end::save-entity[]
            s.save(new DynamoDBEntity(parentId: '1', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(1).toDate()))
            s.saveAll([
                new DynamoDBEntity(parentId: '2', id: '1', rangeIndex: 'foo',  date: REFERENCE_DATE.minusDays(5).toDate()),
                new DynamoDBEntity(parentId: '2', id: '2', rangeIndex: 'foo', date: REFERENCE_DATE.minusDays(2).toDate())])
            s.saveAll(
                new DynamoDBEntity(parentId: '3', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.plusDays(7).toDate()),
                new DynamoDBEntity(parentId: '3', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(14).toDate())
            )

        then:
            noExceptionThrown()
    }

    void 'get items'() {
        expect:
        // tag::load-entity[]
        s.get('1', '1')                                                                 // <4>
        // end::load-entity[]
            s.getAll('1', ['2', '1']).size() == 2
            s.getAll('1', ['2', '1'], [throttle: true]).size() == 2
            s.getAll('1', ['3', '4']).size() == 0
    }

    void 'count items (using java.util.Date)'() {
        expect:
            s.count('1') == 2
            s.count('1', '1') == 1
            s.count('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 1
            s.countByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(1).toDate(),
                before: REFERENCE_DATE.plusDays(2).toDate(),
            ]) == 2
            s.countByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_DATE.minusDays(1).toDate(),
                REFERENCE_DATE.plusDays(2).toDate()
            ) == 2
            s.countByDates('3', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.plusDays(9).toDate(),
                before: REFERENCE_DATE.plusDays(20).toDate(),
            ]) == 1
    }

    void 'count items (using java.time.Instant)'() {
        expect:
            s.count('1') == 2
            s.count('1', '1') == 1
            s.count('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 1
            s.countByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_INSTANT.minus(1, ChronoUnit.DAYS),
                before: REFERENCE_INSTANT.plus(2, ChronoUnit.DAYS),
            ]) == 2
            s.countByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_INSTANT.minus(1, ChronoUnit.DAYS),
                REFERENCE_INSTANT.plus(2, ChronoUnit.DAYS)
            ) == 2
            s.countByDates('3', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_INSTANT.plus(9, ChronoUnit.DAYS),
                before: REFERENCE_INSTANT.plus(20, ChronoUnit.DAYS),
            ]) == 1
    }

    void 'increment and decrement'() {
        when:
        // tag::increment[]
        s.increment('1', '1', 'number')                                                 // <7>
        // end::increment[]
            s.increment('1', '1', 'number')
            s.increment('1', '1', 'number')
            s.decrement('1', '1', 'number')
        then:
            s.get('1', '1').number == 2
    }

    void 'query items (using java.util.Date)'() {
        expect:
            s.query('1').count == 2
            s.query('1', '1').count == 1
            s.query(new DynamoDBQueryExpression().withHashKeyValues(new DynamoDBEntity(parentId: '1'))).size() == 2
            s.query('1', 'range' , 'ANY', null, [returnAll: true, throttle: true])

        // tag::query-by-range-index[]
        s.query('1', DynamoDBEntity.RANGE_INDEX, 'bar').count == 1                      // <5>
        // end::query-by-range-index[]

            s.queryByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(1).toDate(),
                before: REFERENCE_DATE.plusDays(2).toDate(),
            ]).count == 2
            s.queryByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_DATE.minusDays(1).toDate(),
                REFERENCE_DATE.plusDays(2).toDate()
            ).count == 2

        // tag::query-by-dates[]
        s.queryByDates('3', DynamoDBEntity.DATE_INDEX, [                                // <6>
            after: REFERENCE_DATE.plusDays(9).toDate(),
            before: REFERENCE_DATE.plusDays(20).toDate(),
        ]).count == 1
        // end::query-by-dates[]
    }

    void 'query items (using java.time.Instant)'() {
        expect:
            s.query('1').count == 2
            s.query('1', '1').count == 1
            s.query(new DynamoDBQueryExpression().withHashKeyValues(new DynamoDBEntity(parentId: '1'))).size() == 2
            s.query('1', 'range' , 'ANY', null, [returnAll: true, throttle: true])

        // tag::query-by-range-index[]
        s.query('1', DynamoDBEntity.RANGE_INDEX, 'bar').count == 1
        // end::query-by-range-index[]

            s.queryByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_INSTANT.minus(1, ChronoUnit.DAYS),
                before: REFERENCE_INSTANT.plus(2, ChronoUnit.DAYS),
            ]).count == 2
            s.queryByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_INSTANT.minus(1, ChronoUnit.DAYS),
                REFERENCE_INSTANT.plus(2, ChronoUnit.DAYS)
            ).count == 2

        // tag::query-by-dates[]
        s.queryByDates('3', DynamoDBEntity.DATE_INDEX, [
            after: REFERENCE_INSTANT.plus(9, ChronoUnit.DAYS),
            before: REFERENCE_INSTANT.plus(20, ChronoUnit.DAYS),
        ]).count == 1
        // end::query-by-dates[]
    }

    @SuppressWarnings('AbcMetric')
    void 'service introduction works'() {
        given:
            DynamoDBItemDBService s = itemService
        expect:
            s.get('1', '1')
            s.load('1', '1')
            s.getAll('1', ['2', '1']).size() == 2
            s.loadAll('1', ['2', '1']).size() == 2
            s.getAll('1', '2', '1').size() == 2
            s.loadAll('1', '3', '4').size() == 0

            s.save(new DynamoDBEntity(parentId: '1001', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.toDate()))
            s.save(new DynamoDBEntity(parentId: '1001', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(1).toDate()))
            s.saveAll([
                new DynamoDBEntity(parentId: '1002', id: '1', rangeIndex: 'foo',  date: REFERENCE_DATE.minusDays(5).toDate()),
                new DynamoDBEntity(parentId: '1002', id: '2', rangeIndex: 'foo', date: REFERENCE_DATE.minusDays(2).toDate()),
            ])
            s.saveAll(
                new DynamoDBEntity(parentId: '1003', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.plusDays(7).toDate()),
                new DynamoDBEntity(parentId: '1003', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(14).toDate())
            )

            s.count('1') == 2
            s.count('1', '1') == 1
            s.countByRangeIndex('1', 'bar') == 1
            s.countByDates('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()) == 2
            s.countByDates('3', REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 1

            s.query('1').count().blockingGet() == 2
            s.query('1', '1').count().blockingGet() == 1
            s.queryByRangeIndex('1', 'bar').count().blockingGet() == 1
            s.queryByRangeIndex('1', 'bar').blockingSingle().parentId == null // projection
            s.queryByRangeIndex('1', 'bar').blockingSingle().rangeIndex == 'bar' // projection
            s.queryByDates('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()).count().blockingGet() == 2
            s.queryByDatesWithLimit('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate(), 1).count().blockingGet() == 1
            s.queryByDates('3', REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()).count().blockingGet() == 1

            s.scanAllByRangeIndex('bar').count().blockingGet() == 4
            s.scanAllByRangeIndexWithLimit('bar', 2).count().blockingGet() == 2

            s.increment('1001', '1')
            s.increment('1001', '1')
            s.increment('1001', '1')
            s.decrement('1001', '1') == 2
            s.get('1001', '1').number == 2

            s.delete(s.get('1001', '1'))
            s.count('1001', '1') == 0
            s.delete('1003', '1')
            s.count('1003', '1') == 0
            s.deleteByRangeIndex('1001', 'bar') == 1
            s.countByRangeIndex('1001', 'bar') == 0
            s.deleteByDates('1002',  REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 2
            s.countByDates('1002', REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 0
    }

    void 'count many items'() {
        when:
            String parentKey = '2001'
            DynamoDBItemDBService s = itemService
            s.saveAll((1..101).collect { new DynamoDBEntity(parentId: parentKey, id: "$it") })
        then:
            s.count(parentKey) == 101
    }
    void 'delete attribute'() {
        expect:
            s.get('1', '1').number == 2
        when:
            s.deleteItemAttribute('1', '1', 'number')
        then:
            !s.get('1', '1').number
    }

    void 'delete items'() {
        expect:

        // tag::delete[]
        s.delete(s.get('1', '1'))                                                       // <8>
        // end::delete[]

            s.count('1', '1') == 0
            s.delete('3', '1')
            s.count('3', '1') == 0

        // tag::delete-all[]
        s.deleteAll('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 1                        // <9>
        // end::delete-all[]

            s.count('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 0
            s.deleteAllByConditions('2',  DefaultDynamoDBService.buildDateConditions(DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(20).toDate(),
                before: REFERENCE_DATE.plusDays(20).toDate(),
            ]), [limit: 1]) == 2
            s.countByDates('2', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(20).toDate(),
                before: REFERENCE_DATE.plusDays(20).toDate(),
            ]) == 0
    }

}

// tag::service-all[]
// tag::service-header[]
@Service(DynamoDBEntity)                                                                // <2>
interface DynamoDBItemDBService {

// end::service-header[]

    DynamoDBEntity get(String hash, String rangeKey)
    DynamoDBEntity load(String hash, String rangeKey)
    List<DynamoDBEntity> getAll(String hash, List<String> rangeKeys)
    List<DynamoDBEntity> getAll(String hash, String... rangeKeys)
    List<DynamoDBEntity> loadAll(String hash, List<String> rangeKeys)
    List<DynamoDBEntity> loadAll(String hash, String... rangeKeys)

    DynamoDBEntity save(DynamoDBEntity entity)
    List<DynamoDBEntity> saveAll(DynamoDBEntity... entities)
    List<DynamoDBEntity> saveAll(Iterable<DynamoDBEntity> entities)

    int count(String hashKey)
    int count(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range {
                eq DynamoDBEntity.RANGE_INDEX, rangeKey
            }
        }
    })
    int countByRangeIndex(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range { between DynamoDBEntity.DATE_INDEX, after, before }
        }
    })
    int countByDates(String hashKey, Date after, Date before)

    Flowable<DynamoDBEntity> query(String hashKey)
    Flowable<DynamoDBEntity> query(String hashKey, String rangeKey)

    // tag::sample-queries[]
    @Query({                                                                            // <3>
        query(DynamoDBEntity) {
            hash hashKey                                                                // <4>
            range {
                eq DynamoDBEntity.RANGE_INDEX, rangeKey                                 // <5>
            }
            only {                                                                      // <6>
                rangeIndex                                                              // <7>
            }
        }
    })
    Flowable<DynamoDBEntity> queryByRangeIndex(String hashKey, String rangeKey)         // <8>
    // end::sample-queries[]

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range { between DynamoDBEntity.DATE_INDEX, after, before }
        }
    })
    Flowable<DynamoDBEntity> queryByDates(String hashKey, Date after, Date before)

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range { between DynamoDBEntity.DATE_INDEX, after, before }
            limit max
        }
    })
    Flowable<DynamoDBEntity> queryByDatesWithLimit(String hashKey, Date after, Date before, int max)

    void delete(DynamoDBEntity entity)
    void delete(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range {
                eq DynamoDBEntity.RANGE_INDEX, rangeKey
            }
        }
    })
    int deleteByRangeIndex(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            hash hashKey
            range { between DynamoDBEntity.DATE_INDEX, after, before }
        }
    })
    int deleteByDates(String hashKey, Date after, Date before)

    // tag::sample-update[]
    @Update({                                                                           // <3>
        update(DynamoDBEntity) {
            hash hashKey                                                                // <4>
            range rangeKey                                                              // <5>
            add 'number', 1                                                             // <6>
            returnUpdatedNew { number }                                                 // <7>
        }
    })
    Number increment(String hashKey, String rangeKey)                                   // <8>
    // end::sample-update[]

    @Update({
        update(DynamoDBEntity) {
            hash hashKey
            range rangeKey
            add 'number', -1
            returnUpdatedNew { number }
        }
    })
    Number decrement(String hashKey, String rangeKey)

    // tag::sample-scan[]
    @Scan({                                                                             // <3>
        scan(DynamoDBEntity) {
            filter {
                eq DynamoDBEntity.RANGE_INDEX, foo                                      // <4>
            }
        }
    })
    Flowable<DynamoDBEntity> scanAllByRangeIndex(String foo)                            // <5>
    // end::sample-scan[]

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                eq DynamoDBEntity.RANGE_INDEX, foo
            }
            limit max
        }
    })
    Flowable<DynamoDBEntity> scanAllByRangeIndexWithLimit(String foo, int max)

// tag::service-footer[]

}
// end::service-footer[]
// end::service-all[]
