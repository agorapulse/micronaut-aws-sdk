package com.agorapulse.micronaut.aws.sns;

import com.agorapulse.micronaut.aws.Pogo;
import com.agorapulse.micronaut.aws.sns.annotation.NotificationClient;
import com.agorapulse.micronaut.aws.sns.annotation.Topic;

import java.util.Map;

@NotificationClient                                                                     // <1>
interface DefaultClient {

    String OTHER_TOPIC = "OtherTopic";

    @Topic("OtherTopic") String publishMessageToDifferentTopic(Pogo pogo);              // <2>

    String publishMessage(Pogo message);                                                // <3>
    String publishMessage(String subject, Pogo message);                                // <4>
    String publishMessage(String message);                                              // <5>
    String publishMessage(String subject, String message);

    String sendSMS(String phoneNumber, String message);                                 // <6>
    String sendSms(String phoneNumber, String message, Map attributes);                 // <7>

}
