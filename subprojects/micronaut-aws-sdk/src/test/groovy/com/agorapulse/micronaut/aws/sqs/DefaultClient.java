package com.agorapulse.micronaut.aws.sqs;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sqs.annotation.Queue;
import com.agorapulse.micronaut.aws.sqs.annotation.QueueClient;

@QueueClient                                                                            // <1>
interface DefaultClient {

    @Queue(value = "OtherQueue", group = "SomeGroup")
    String sendMessageToQueue(String message);                                          // <2>

    String sendMessage(Pogo message);                                                   // <3>

    String sendMessage(byte[] record);                                                  // <4>

    String sendMessage(String record);                                                  // <5>

    String sendMessage(String record, int delay);                                       // <6>

    String sendMessage(String record, String group);                                    // <7>

    String sendMessage(String record, int delay, String group);                         // <8>

    void deleteMessage(String messageId);                                               // <9>

    String OTHER_QUEUE = "OtherQueue";
}
