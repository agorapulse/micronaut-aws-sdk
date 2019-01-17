package com.agorapulse.micronaut.aws.apigateway.ws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import io.micronaut.function.FunctionBean;

import java.util.function.Consumer;

@FunctionBean("notification-handler")
public class NotificationHandler implements Consumer<SNSEvent> {

    private final MessageSender sender;                                                 // <1>

    public NotificationHandler(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void accept(SNSEvent event) {                                                // <2>
        event.getRecords().forEach(it -> {
            try {
                sender.send(                                                            // <3>
                    it.getSNS().getSubject(),
                    "[SNS] " + it.getSNS().getMessage()
                );
            } catch (AmazonClientException ignored) {
                // can be gone                                                          // <4>
            }
        });
    }

}
