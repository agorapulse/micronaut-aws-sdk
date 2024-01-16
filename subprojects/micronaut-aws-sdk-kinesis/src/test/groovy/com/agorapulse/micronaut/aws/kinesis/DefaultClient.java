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
package com.agorapulse.micronaut.aws.kinesis;

import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisClient;
import com.agorapulse.micronaut.aws.kinesis.annotation.PartitionKey;
import com.agorapulse.micronaut.aws.kinesis.annotation.SequenceNumber;
import com.agorapulse.micronaut.aws.kinesis.annotation.Stream;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;

// tag::header[]
@KinesisClient                                                                          // <1>
interface DefaultClient {
// end::header[]

    String OTHER_STREAM = "OtherStream";

    //CHECKSTYLE:OFF
    // tag::string[]
    void putRecordString(String record);                                                // <2>

    PutRecordResult putRecord(String partitionKey, String record);                      // <3>

    void putRecordAnno(@PartitionKey String id, String record);                         // <4>

    void putRecord(String partitionKey, String record, String sequenceNumber);          // <5>

    void putRecordAnno(                                                                 // <6>
        @PartitionKey String id,
        String record,
        @SequenceNumber String sqn
    );

    void putRecordAnnoNumbers(                                                          // <7>
        @PartitionKey Long id,
        String record,
        @SequenceNumber int sequenceNumber
    );
    // end::string[]
    //CHECKSTYLE:ON

    // tag::bytes[]
    void putRecordBytes(byte[] record);                                                 // <2>

    void putRecordDataByteArray(@PartitionKey String id, byte[] value);                 // <3>

    PutRecordsResult putRecords(Iterable<PutRecordsRequestEntry> entries);              // <4>

    PutRecordsResult putRecords(PutRecordsRequestEntry... entries);                     // <5>

    PutRecordsResult putRecord(PutRecordsRequestEntry entry);                           // <6>
    // end::bytes[]

    // tag::pogo[]
    void putRecordObject(Pogo pogo);                                                    // <2>

    PutRecordsResult putRecordObjects(Pogo... pogo);                                    // <3>

    PutRecordsResult putRecordObjects(Iterable<Pogo> pogo);                             // <4>

    void putRecordDataObject(@PartitionKey String id, Pogo value);                      // <5>
    // end::pogo[]


    // tag::events[]
    PutRecordResult putEvent(MyEvent event);                                            // <2>

    PutRecordsResult putEventsIterable(Iterable<MyEvent> events);                       // <3>

    void putEventsArrayNoReturn(MyEvent... events);                                     // <4>

    @Stream("OtherStream") PutRecordResult putEventToStream(MyEvent event);             // <5>
    // end::events[]

}
