/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
                String connectionId = it.getSNS().getSubject();
                String payload = "[SNS] " + it.getSNS().getMessage();
                sender.send(connectionId, payload);                                     // <3>
            } catch (AmazonClientException ignored) {
                // can be gone                                                          // <4>
            }
        });
    }

}
