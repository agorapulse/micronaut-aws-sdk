package com.agorapulse.micronaut.aws.sns;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sns.annotation.NotificationClient;

@NotificationClient(topic = "SomeTopic") interface StreamClient {

    String SOME_STREAM = "SomeTopic";

    String publishMessage(Pogo message);

}
