package com.agorapulse.micronaut.aws.apigateway.ws

import com.amazonaws.AmazonClientException
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import groovy.transform.Field

import javax.inject.Inject

@Inject @Field MessageSender sender                                                     // <1>

void notify(SNSEvent event) {                                                           // <2>
    event.records.each {
        try {
            sender.send(it.SNS.subject, "[SNS] $it.SNS.message")                        // <3>
        } catch (AmazonClientException ignored) {
            // can be gone                                                              // <4>
        }
    }
}
