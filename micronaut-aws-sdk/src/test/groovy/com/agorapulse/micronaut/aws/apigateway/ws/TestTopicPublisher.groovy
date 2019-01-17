package com.agorapulse.micronaut.aws.apigateway.ws

import com.agorapulse.micronaut.aws.sns.annotation.NotificationClient
import groovy.transform.CompileStatic

@CompileStatic
@NotificationClient
interface TestTopicPublisher {
    String publishMessage(String subject, String message);
}
