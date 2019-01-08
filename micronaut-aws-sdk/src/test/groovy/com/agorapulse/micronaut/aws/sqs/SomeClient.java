package com.agorapulse.micronaut.aws.sqs;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sqs.annotation.QueueClient;

@QueueClient(queue = "SomeQueue", delay = 10)
public interface SomeClient {

    String sendMessage(Pogo event);

    String SOME_QUEUE = "SomeQueue";
}
