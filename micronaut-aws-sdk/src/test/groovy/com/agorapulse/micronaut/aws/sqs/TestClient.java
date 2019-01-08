package com.agorapulse.micronaut.aws.sqs;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sqs.annotation.QueueClient;

@QueueClient("test")
interface TestClient {
    String sendMessage(Pogo event);

    void doWhatever(Object one, Object two, Object three, Object four);
}
