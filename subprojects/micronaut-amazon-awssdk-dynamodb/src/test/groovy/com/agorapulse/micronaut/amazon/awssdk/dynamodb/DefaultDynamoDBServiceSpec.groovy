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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Scan
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update
import io.micronaut.context.ApplicationContext
import io.reactivex.Flowable
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Instant

import static com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.Builders.*

// tag::builders-import[]

// <1>

// end::builders-import[]

/**
 * Specification for testing DefaultDynamoDBService using entity with range key.
 */
// tag::testcontainers-header[]
@Stepwise
@Testcontainers                                                                         // <1>
class DefaultDynamoDBServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final Instant REFERENCE_DATE = Instant.ofEpochMilli(1358487600000)

    // tag::testcontainers-setup[]
    @AutoCleanup ApplicationContext context                                             // <2>

    @Shared LocalStackV2Container localstack = new LocalStackV2Container()                  // <3>
        .withServices(LocalStackV2Container.Service.DYNAMODB)

    DynamoDBItemDBService service

    void setup() {
        DynamoDbClient client = DynamoDbClient                                          // <4>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackV2Container.Service.DYNAMODB))
            .credentialsProvider(localstack.getDefaultCredentialsProvider())
            .region(Region.EU_WEST_1)
            .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
            .builder()
            .dynamoDbClient(client)
            .build();

        context = ApplicationContext.build().build()
        context.registerSingleton(DynamoDbClient, client)                               // <5>
        context.registerSingleton(DynamoDbEnhancedClient, enhancedClient)               // <6>
        context.start()

        service = context.getBean(DynamoDBItemDBService)                                      // <7>
    }

    @SuppressWarnings('AbcMetric')
    void 'service introduction works'() {
        expect:
            service.get('1', '1')
            service.load('1', '1')
            service.getAll('1', ['2', '1']).size() == 2
            service.loadAll('1', ['2', '1']).size() == 2
            service.getAll('1', '2', '1').size() == 2
            service.loadAll('1', '3', '4').size() == 0

            service.save(new DynamoDBEntity(parentId: '1001', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.toDate()))
            service.save(new DynamoDBEntity(parentId: '1001', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(1).toDate()))
            service.saveAll([
                new DynamoDBEntity(parentId: '1002', id: '1', rangeIndex: 'foo',  date: REFERENCE_DATE.minusDays(5).toDate()),
                new DynamoDBEntity(parentId: '1002', id: '2', rangeIndex: 'foo', date: REFERENCE_DATE.minusDays(2).toDate()),
            ])
            service.saveAll(
                new DynamoDBEntity(parentId: '1003', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.plusDays(7).toDate()),
                new DynamoDBEntity(parentId: '1003', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(14).toDate())
            )

            service.count('1') == 2
            service.count('1', '1') == 1
            service.countByRangeIndex('1', 'bar') == 1
            service.countByDates('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()) == 2
            service.countByDates('3', REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 1

            service.query('1').count().blockingGet() == 2
            service.query('1', '1').count().blockingGet() == 1
            service.queryByRangeIndex('1', 'bar').count().blockingGet() == 1
            service.queryByRangeIndex('1', 'bar').blockingSingle().parentId == null // projection
            service.queryByRangeIndex('1', 'bar').blockingSingle().rangeIndex == 'bar' // projection
            service.queryByDates('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()).count().blockingGet() == 2
            service.queryByDatesWithLimit('1', REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate(), 1).count().blockingGet() == 1
            service.queryByDates('3', REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()).count().blockingGet() == 1

            service.scanAllByRangeIndex('bar').count().blockingGet() == 4
            service.scanAllByRangeIndexWithLimit('bar', 2).count().blockingGet() == 2

            service.increment('1001', '1')
            service.increment('1001', '1')
            service.increment('1001', '1')
            service.decrement('1001', '1') == 2
            service.get('1001', '1').number == 2

            service.delete(service.get('1001', '1'))
            service.count('1001', '1') == 0
            service.delete('1003', '1')
            service.count('1003', '1') == 0
            service.deleteByRangeIndex('1001', 'bar') == 1
            service.countByRangeIndex('1001', 'bar') == 0
            service.deleteByDates('1002',  REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 2
            service.countByDates('1002', REFERENCE_DATE.minusDays(20).toDate(), REFERENCE_DATE.plusDays(20).toDate()) == 0
    }

    void 'count many items'() {
        when:
            String parentKey = '2001'
            service.saveAll((1..101).collect { new DynamoDBEntity(parentId: parentKey, id: "$it") })
        then:
            service.count(parentKey) == 101
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
