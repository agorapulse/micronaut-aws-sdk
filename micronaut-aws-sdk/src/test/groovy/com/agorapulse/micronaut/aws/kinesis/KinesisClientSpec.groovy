package com.agorapulse.micronaut.aws.kinesis

import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisClient
import com.agorapulse.micronaut.aws.kinesis.annotation.PartitionKey
import com.agorapulse.micronaut.aws.kinesis.annotation.SequenceNumber
import com.amazonaws.services.kinesis.model.PutRecordResult
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry
import com.amazonaws.services.kinesis.model.PutRecordsResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

class KinesisClientSpec extends Specification {

    private static final String PARTITION_KEY = '1234567890'
    private static final String SEQUENCE_NUMBER = '987654321'
    private static final String RECORD = "Record"
    private static final MyEvent EVENT_1 = new MyEvent(value: 'foo')
    private static final MyEvent EVENT_2 = new MyEvent(value: 'bar')
    private static final Pogo POGO = new Pogo(foo: 'bar')
    private static final PutRecordsRequestEntry PUT_RECORDS_REQUEST_ENTRY = new PutRecordsRequestEntry()
    private static final PutRecordResult PUT_RECORD_RESULT = new PutRecordResult().withSequenceNumber('1')
    private static final PutRecordsResult PUT_RECORDS_RESULT = new PutRecordsResult()

    KinesisService defaultService = Mock(KinesisService)
    KinesisService testService = Mock(KinesisService)

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.build().build()

        context.registerSingleton(KinesisService, defaultService, Qualifiers.byName('default'))
        context.registerSingleton(KinesisService, testService, Qualifiers.byName('test'))

        context.start()
    }

    void 'can put single event and return void'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResult result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putEvent(EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put single event and return void with specified configuration name'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            PutRecordResult result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * testService.putEvent(EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put multiple events and return put records results'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordsResult results = client.putEventsIterable([EVENT_1, EVENT_2])
        then:
            results == PUT_RECORDS_RESULT

            1 * defaultService.putEvents([EVENT_1, EVENT_2]) >> PUT_RECORDS_RESULT
    }

    void 'can put array of events and return void'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putEventsArrayNoReturn(EVENT_1, EVENT_2)
        then:
            1 * defaultService.putEvents([EVENT_1, EVENT_2]) >> PUT_RECORDS_RESULT
    }

    void 'cannot put just single byte array'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordBytes(RECORD.toString().bytes)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'cannot put just single string'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordString(RECORD)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'cannot put just single pojo'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordObject(POGO)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'needs to follow the method convention rules'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.doWhatever(POGO, PARTITION_KEY, SEQUENCE_NUMBER, PUT_RECORDS_REQUEST_ENTRY)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'can put record with partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResult result = client.putRecord(PARTITION_KEY, RECORD)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putRecord(PARTITION_KEY, RECORD, null) >> PUT_RECORD_RESULT
    }

    void 'can put record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnno(PARTITION_KEY, RECORD)
        then:
            1 * defaultService.putRecord(PARTITION_KEY, RECORD, null) >> PUT_RECORD_RESULT
    }

    void 'can put byte array record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordDataByteArray(PARTITION_KEY, RECORD.bytes)
        then:
            1 * defaultService.putRecord(PARTITION_KEY, RECORD.bytes, null) >> PUT_RECORD_RESULT
    }

    void 'can put pogo record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordDataObject(PARTITION_KEY, POGO)
        then:
            1 * defaultService.putRecord(PARTITION_KEY, context.getBean(ObjectMapper).writeValueAsBytes(POGO), null) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and sequence number'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecord(PARTITION_KEY, RECORD, SEQUENCE_NUMBER)
        then:
            1 * defaultService.putRecord(PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and annotated sequence number'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnno(PARTITION_KEY, RECORD, SEQUENCE_NUMBER)
        then:
            1 * defaultService.putRecord(PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and annotated sequence number of any type'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnnoNumbers(PARTITION_KEY.toLong(), RECORD, SEQUENCE_NUMBER.toInteger())
        then:
            1 * defaultService.putRecord(PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put records entries as iterable'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecords([PUT_RECORDS_REQUEST_ENTRY])
        then:
            1 * defaultService.putRecords([PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }

    void 'can put records entries as array'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecords(PUT_RECORDS_REQUEST_ENTRY)
        then:
            1 * defaultService.putRecords([PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }

    void 'can put records entries as single argument'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecord(PUT_RECORDS_REQUEST_ENTRY)
        then:
            1 * defaultService.putRecords([PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }
}

@KinesisClient interface DefaultClient {

    PutRecordResult putEvent(MyEvent event)
    PutRecordsResult putEventsIterable(Iterable<MyEvent> events)
    void putEventsArrayNoReturn(MyEvent... events)

    // failing
    void putRecordBytes(byte[] record)
    void putRecordString(String record)
    void putRecordObject(Pogo pogo)
    void doWhatever(Object one, Object two, Object three, Object four)

    PutRecordResult putRecord(String partitionKey, String record)
    void putRecordAnno(@PartitionKey String id, String record)
    void putRecordDataByteArray(@PartitionKey String id, byte[] value)
    void putRecordDataObject(@PartitionKey String id, Pogo value)
    void putRecord(String partitionKey, String record, String sequenceNumber)
    void putRecordAnno(@PartitionKey String id, String record, @SequenceNumber String sqn)
    void putRecordAnnoNumbers(@PartitionKey Long id, String record, @SequenceNumber int sequenceNumber)
    PutRecordsResult putRecords(Iterable<PutRecordsRequestEntry> entries)
    PutRecordsResult putRecords(PutRecordsRequestEntry... entries)
    PutRecordsResult putRecord(PutRecordsRequestEntry entry)

}

@KinesisClient('test') interface TestClient {
    PutRecordResult putEvent(MyEvent event)
}

@KinesisClient(stream = 'SomeStream') interface StreamClient {

}

class Pogo {
    String foo
}
