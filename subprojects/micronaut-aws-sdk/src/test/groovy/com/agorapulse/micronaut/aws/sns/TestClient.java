package com.agorapulse.micronaut.aws.sns;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sns.annotation.NotificationClient;

@NotificationClient("test") interface TestClient {
    String publishMessage(Pogo message);
}
