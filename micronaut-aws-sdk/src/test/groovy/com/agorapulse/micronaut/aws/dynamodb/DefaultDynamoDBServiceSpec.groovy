package com.agorapulse.micronaut.aws.dynamodb


import com.agorapulse.micronaut.aws.dynamodb.query.QueryBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import io.reactivex.Flowable
import org.joda.time.DateTime
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import space.jasan.support.groovy.closure.ConsumerWithDelegate
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
@Testcontainers
class DefaultDynamoDBServiceSpec extends Specification {

    private static final DateTime REFERENCE_DATE = new DateTime(1358487600000)

    @Shared LocalStackContainer localstack = new LocalStackContainer().withServices(LocalStackContainer.Service.DYNAMODB)

    DefaultDynamoDBService<DynamoDBEntity> service
    AmazonDynamoDB amazonDynamoDB
    IDynamoDBMapper mapper

    void setup() {
        amazonDynamoDB = AmazonDynamoDBClient
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        mapper = new DynamoDBMapper(amazonDynamoDB)

        service = new DefaultDynamoDBService<>(this.amazonDynamoDB, new DynamoDBMapper(this.amazonDynamoDB), DynamoDBEntity)
    }

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
            service.hashKeyName == 'parentId'
            service.hashKeyClass == String
            service.rangeKeyName == 'id'
            service.rangeKeyClass == String
            service.isIndexRangeKey(DynamoDBEntity.RANGE_INDEX)
            service.isIndexRangeKey(DynamoDBEntity.DATE_INDEX)
            service.newInstance instanceof DynamoDBEntity
    }

    void 'create table'() {
        when:
            CreateTableResult result = service.createTable()
        then:
            result.tableDescription.tableName == 'entity'

        when:
            CreateTableResult none = service.createTable()
        then:
            !none
    }

    void 'save items'() {
        when:
            service.save(new DynamoDBEntity(parentId: '1', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.toDate()))
            service.save(new DynamoDBEntity(parentId: '1', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(1).toDate()))
            service.saveAll([
                new DynamoDBEntity(parentId: '2', id: '1', rangeIndex: 'foo',  date: REFERENCE_DATE.minusDays(5).toDate()),
                new DynamoDBEntity(parentId: '2', id: '2', rangeIndex: 'foo', date: REFERENCE_DATE.minusDays(2).toDate())])
            service.saveAll(
                new DynamoDBEntity(parentId: '3', id: '1', rangeIndex: 'foo', date: REFERENCE_DATE.plusDays(7).toDate()),
                new DynamoDBEntity(parentId: '3', id: '2', rangeIndex: 'bar', date: REFERENCE_DATE.plusDays(14).toDate())
            )

        then:
            noExceptionThrown()
    }

    void 'get items'() {
        expect:
            service.get('1', '1')
            service.getAll('1', ['2', '1']).size() == 2
            service.getAll('1', ['2', '1'], [throttle: true]).size() == 2
            service.getAll('1', ['3', '4']).size() == 0
    }

    void 'count items'() {
        expect:
            service.count('1') == 2
            service.count('1', '1') == 1
            service.count('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 1
            service.countByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(1).toDate(), before: REFERENCE_DATE.plusDays(2).toDate()]
            ) == 2
            service.countByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_DATE.minusDays(1).toDate(),
                REFERENCE_DATE.plusDays(2).toDate()
            ) == 2
            service.countByDates('3', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.plusDays(9).toDate(), before: REFERENCE_DATE.plusDays(20).toDate()
            ]) == 1

    }

    void 'increment and decrement'() {
        when:
            service.increment('1', '1', 'number')
            service.increment('1', '1', 'number')
            service.increment('1', '1', 'number')
            service.decrement('1', '1', 'number')
        then:
            service.get('1', '1').number == 2

    }

    void 'query items'() {
        expect:
            service.query('1').count == 2
            service.query('1', '1').count == 1
            service.query(new DynamoDBQueryExpression().withHashKeyValues(new DynamoDBEntity(parentId: '1'))).size() == 2
            service.query('1', 'range' , 'ANY', null, [returnAll: true, throttle: true])
            service.query('1', DynamoDBEntity.RANGE_INDEX, 'bar').count == 1

            service.queryByDates('1', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(1).toDate(), before: REFERENCE_DATE.plusDays(2).toDate()]
            ).count == 2
            service.queryByDates(
                '1',
                DynamoDBEntity.DATE_INDEX,
                REFERENCE_DATE.minusDays(1).toDate(),
                REFERENCE_DATE.plusDays(2).toDate()
            ).count == 2
            service.queryByDates('3', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.plusDays(9).toDate(), before: REFERENCE_DATE.plusDays(20).toDate()
            ]).count == 1
    }

    void 'query with builder'() {
        expect:
            query {
                hash '1'
            }.count().blockingGet() == 2

            query {
                hash '1'
                range {
                    eq '1'
                }
            }.count().blockingGet() == 1

            query {
                hash '1'
                range {
                    eq DynamoDBEntity.RANGE_INDEX, 'bar'
                }
            }.count().blockingGet() == 1

            query {
                hash '1'
                range {
                    between DynamoDBEntity.DATE_INDEX, REFERENCE_DATE.minusDays(1).toDate(), REFERENCE_DATE.plusDays(2).toDate()
                }
            }.count().blockingGet() == 2

            query {
                hash '3'
                range {
                    between DynamoDBEntity.DATE_INDEX, REFERENCE_DATE.plusDays(9).toDate(), REFERENCE_DATE.plusDays(20).toDate()
                }
            }.count().blockingGet() == 1
    }

    void 'delete attribute'() {
        expect:
            service.get('1', '1').number == 2
        when:
            service.deleteItemAttribute('1', '1', 'number')
        then:
            !service.get('1', '1').number
    }

    void 'delete items'() {
        expect:
            service.delete(service.get('1', '1'))
            service.count('1', '1') == 0
            service.delete('3', '1')
            service.count('3', '1') == 0
            service.deleteAll('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 1
            service.count('1', DynamoDBEntity.RANGE_INDEX, 'bar') == 0
            service.deleteAllByConditions('2',  DefaultDynamoDBService.buildDateConditions(DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(20).toDate(), before: REFERENCE_DATE.plusDays(20).toDate()
            ]), [limit: 1]) == 2
            service.countByDates('2', DynamoDBEntity.DATE_INDEX, [
                after: REFERENCE_DATE.minusDays(20).toDate(), before: REFERENCE_DATE.plusDays(20).toDate()]
            ) == 0
    }

    private Flowable<DynamoDBEntity> query(
        @DelegatesTo(value = QueryBuilder.class, strategy = Closure.DELEGATE_FIRST, genericTypeIndex = 0)
        @ClosureParams(value = SimpleType.class, options = "com.agorapulse.micronaut.aws.dynamodb.query.QueryBuilder<DynamoDBEntity>")
            Closure<QueryBuilder<DynamoDBEntity>> definition
    ) {
        return QueryBuilder.query(DynamoDBEntity, ConsumerWithDelegate.create(definition)).query(mapper)
    }

}
