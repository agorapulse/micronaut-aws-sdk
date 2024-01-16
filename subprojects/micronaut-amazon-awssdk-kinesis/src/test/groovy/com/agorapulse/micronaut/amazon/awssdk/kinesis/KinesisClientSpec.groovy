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
package com.agorapulse.micronaut.amazon.awssdk.kinesis

import com.agorapulse.micronaut.amazon.awssdk.kinesis.annotation.KinesisClient
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileDynamic
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Tests for Kinesis declarative clients.
 */
@CompileDynamic
class KinesisClientSpec extends Specification {

    private static final String DEFAULT_STREAM_NAME = 'DefaultStream'
    private static final String PARTITION_KEY = '1234567890'
    private static final String SEQUENCE_NUMBER = '987654321'
    private static final String RECORD = 'Record'
    private static final MyEvent EVENT_1 = new MyEvent(value: 'foo')
    private static final MyEvent EVENT_2 = new MyEvent(value: 'bar')
    private static final Pogo POGO_1 = new Pogo(foo: 'bar')
    private static final Pogo POGO_2 = new Pogo(foo: 'baz')
    private static final PutRecordsRequestEntry PUT_RECORDS_REQUEST_ENTRY = PutRecordsRequestEntry.builder().build()
    private static final PutRecordResponse PUT_RECORD_RESULT = PutRecordResponse.builder().sequenceNumber('1').build()
    private static final PutRecordsResponse PUT_RECORDS_RESULT = PutRecordsResponse.builder().build()

    KinesisService defaultService = Mock(KinesisService) {
        getDefaultStreamName() >> DEFAULT_STREAM_NAME
    }

    KinesisService testService = Mock(KinesisService) {
        getDefaultStreamName() >> DEFAULT_STREAM_NAME
    }

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.builder().build()

        context.registerSingleton(KinesisService, defaultService)
        context.registerSingleton(KinesisService, testService, Qualifiers.byName('test'))

        context.start()
    }

    void 'can put single event and return void'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResponse result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putEvent(DEFAULT_STREAM_NAME, EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put single event and return void with specified configuration name'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            PutRecordResponse result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * testService.putEvent(DEFAULT_STREAM_NAME, EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put single event and return void with specified stream name'() {
        given:
            StreamClient client = context.getBean(StreamClient)
        when:
            PutRecordResponse result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putEvent(StreamClient.SOME_STREAM, EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put single event and return void with specified stream name in the annotation'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResponse result = client.putEventToStream(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putEvent(DefaultClient.OTHER_STREAM, EVENT_1) >> PUT_RECORD_RESULT
    }

    void 'can put multiple events and return put records results'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordsResponse results = client.putEventsIterable([EVENT_1, EVENT_2])
        then:
            results == PUT_RECORDS_RESULT

            1 * defaultService.putEvents(DEFAULT_STREAM_NAME, [EVENT_1, EVENT_2]) >> PUT_RECORDS_RESULT
    }

    void 'can put array of events and return void'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putEventsArrayNoReturn(EVENT_1, EVENT_2)
        then:
            1 * defaultService.putEvents(DEFAULT_STREAM_NAME, [EVENT_1, EVENT_2]) >> PUT_RECORDS_RESULT
    }

    void 'can put just single byte array'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordBytes(RECORD.toString().bytes)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, _ as String, RECORD.bytes) >> PUT_RECORD_RESULT
    }

    void 'can put just single string'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordString(RECORD)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, _ as String, RECORD) >> PUT_RECORD_RESULT
    }

    void 'can put just single pojo'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordObject(POGO_1)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, _ as String, json(POGO_1), null) >> PUT_RECORD_RESULT
    }

    void 'can put multiple pogos and return put records results'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordsResponse results = client.putRecordObjects([POGO_1, POGO_2])
        then:
            results == PUT_RECORDS_RESULT

            1 * defaultService.putRecords(DEFAULT_STREAM_NAME, _) >> PUT_RECORDS_RESULT
    }

    void 'can put array of pogos and return put records results'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordsResponse results = client.putRecordObjects(POGO_1, POGO_2)
        then:
            results == PUT_RECORDS_RESULT

            1 * defaultService.putRecords(DEFAULT_STREAM_NAME, _) >> PUT_RECORDS_RESULT
    }

    void 'needs to follow the method convention rules'() {
        given:
            TestClient client = context.getBean(TestClient)
        when:
            client.doWhatever(POGO_1, PARTITION_KEY, SEQUENCE_NUMBER, PUT_RECORDS_REQUEST_ENTRY)
        then:
            thrown(UnsupportedOperationException)
    }

    void 'can put record with partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResponse result = client.putRecord(PARTITION_KEY, RECORD)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD, null) >> PUT_RECORD_RESULT
    }

    void 'can put record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnno(PARTITION_KEY, RECORD)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD, null) >> PUT_RECORD_RESULT
    }

    void 'can put byte array record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordDataByteArray(PARTITION_KEY, RECORD.bytes)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD.bytes, null) >> PUT_RECORD_RESULT
    }

    void 'can put pogo record with annotated partition key'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordDataObject(PARTITION_KEY, POGO_1)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, json(POGO_1), null) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and sequence number'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecord(PARTITION_KEY, RECORD, SEQUENCE_NUMBER)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and annotated sequence number'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnno(PARTITION_KEY, RECORD, SEQUENCE_NUMBER)
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put record with partition key and annotated sequence number of any type'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecordAnnoNumbers(PARTITION_KEY.toLong(), RECORD, SEQUENCE_NUMBER.toInteger())
        then:
            1 * defaultService.putRecord(DEFAULT_STREAM_NAME, PARTITION_KEY, RECORD, SEQUENCE_NUMBER) >> PUT_RECORD_RESULT
    }

    void 'can put records entries as iterable'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecords([PUT_RECORDS_REQUEST_ENTRY])
        then:
            1 * defaultService.putRecords(DEFAULT_STREAM_NAME, [PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }

    void 'can put records entries as array'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecords(PUT_RECORDS_REQUEST_ENTRY)
        then:
            1 * defaultService.putRecords(DEFAULT_STREAM_NAME, [PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }

    void 'can put records entries as single argument'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            client.putRecord(PUT_RECORDS_REQUEST_ENTRY)
        then:
            1 * defaultService.putRecords(DEFAULT_STREAM_NAME, [PUT_RECORDS_REQUEST_ENTRY]) >> PUT_RECORDS_RESULT
    }

    void 'stream in created automatically'() {
        given:
            DefaultClient client = context.getBean(DefaultClient)
        when:
            PutRecordResponse result = client.putEvent(EVENT_1)
        then:
            result == PUT_RECORD_RESULT

            1 * defaultService.putEvent(DEFAULT_STREAM_NAME, EVENT_1) >> { throw ResourceNotFoundException.builder().build() }
            1 * defaultService.createStream(DEFAULT_STREAM_NAME)
            1 * defaultService.putEvent(DEFAULT_STREAM_NAME, EVENT_1) >> PUT_RECORD_RESULT
    }

    private byte[] json(Object object) {
        return context.getBean(ObjectMapper).writeValueAsBytes(object)
    }

}

@KinesisClient('test')
interface TestClient {

    PutRecordResponse putEvent(MyEvent event)
    void doWhatever(Object one, Object two, Object three, Object four)

}

@KinesisClient(stream = 'SomeStream')
interface StreamClient {

    public String SOME_STREAM = 'SomeStream'

    PutRecordResponse putEvent(MyEvent event)

}

