/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.aws.sns;

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
