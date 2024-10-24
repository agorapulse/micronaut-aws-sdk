/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.sqs.groovy;

import com.agorapulse.micronaut.amazon.awssdk.sqs.SimpleQueueService;
import com.agorapulse.micronaut.amazon.awssdk.sqs.SimpleQueueServiceConfiguration;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;

/**
 * Amazon SQS services
 *
 * @see SimpleQueueServiceConfiguration
 */
public class SimpleQueueServiceExtensions {

    /**
     * Sends message with additional configuration into the default queue.
     *
     * @param messageBody          message body
     * @param messageConfiguration additional configuration
     * @return message id
     */
    public static String sendMessage(
        SimpleQueueService self,
        String messageBody,
        @DelegatesTo(value = SendMessageRequest.Builder.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.sqs.model.SendMessageRequest.Builder")
            Closure<?> messageConfiguration
    ) {
        return self.sendMessage(self.getDefaultQueueName(), messageBody, ConsumerWithDelegate.create(messageConfiguration));
    }

    /**
     * Sends message with additional configuration into the given queue.
     *
     * @param queueName            name of the queue
     * @param messageBody          message body
     * @param messageConfiguration additional configuration
     * @return message id
     */
    public static String sendMessage(
        SimpleQueueService self,
        String queueName,
        String messageBody,
        @DelegatesTo(value = SendMessageRequest.Builder.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.sqs.model.SendMessageRequest.Builder")
            Closure<?> messageConfiguration
    ) {
        return self.sendMessage(queueName, messageBody, ConsumerWithDelegate.create(messageConfiguration));
    }

    /**
     * Sends message with additional configuration into the default queue.
     *
     * @param messageBodies        message bodies
     * @param messageConfiguration additional configuration
     * @return message id
     */
    public static Publisher<String> sendMessages(
        SimpleQueueService self,
        Publisher<String> messageBodies,
        @DelegatesTo(value = SendMessageBatchRequestEntry.Builder.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry.Builder")
        Closure<?> messageConfiguration
    ) {
        return self.sendMessages(self.getDefaultQueueName(), messageBodies, ConsumerWithDelegate.create(messageConfiguration));
    }

    /**
     * Sends message with additional configuration into the given queue.
     *
     * @param queueName            name of the queue
     * @param messageBodies        message bodies
     * @param messageConfiguration additional configuration
     * @return message id
     */
    public static Publisher<String> sendMessages(
        SimpleQueueService self,
        String queueName,
        Publisher<String> messageBodies,
        @DelegatesTo(value = SendMessageRequest.Builder.class, strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry.Builder")
        Closure<?> messageConfiguration
    ) {
        return self.sendMessages(queueName, messageBodies, ConsumerWithDelegate.create(messageConfiguration));
    }

}
