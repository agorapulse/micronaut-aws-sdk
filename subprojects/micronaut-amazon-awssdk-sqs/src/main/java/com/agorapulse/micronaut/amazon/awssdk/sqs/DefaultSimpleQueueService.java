/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.sqs;

import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default simple queue service implementation.
 */
public class DefaultSimpleQueueService implements SimpleQueueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleQueueService.class);

    private final SqsClient client;
    private final SimpleQueueServiceConfiguration configuration;

    private final ConcurrentMap<String, String> queueUrlByNames = new ConcurrentHashMap<>();

    public DefaultSimpleQueueService(
        SqsClient client,
        SimpleQueueServiceConfiguration configuration
    ) {
        this.client = client;
        this.configuration = configuration;
    }

    @Override
    public String getDefaultQueueName() {
        assertDefaultQueueName();
        return configuration.getQueue();
    }

    @Override
    public boolean isCaching() {
        return configuration.isCache();
    }

    /**
     * @param queueName the name of the queue
     * @return queue URL
     */
    public String createQueue(String queueName) {
        QueueConfiguration configuration = this.configuration.copy();
        configuration.setQueue(queueName);

        if (queueName.endsWith(".fifo")) {
            configuration.setFifo(true);
        }

        return createQueue(configuration);
    }

    /**
     * @param configuration the configuration of the queue
     * @return queue URL
     */
    public String createQueue(QueueConfiguration configuration) {
        CreateQueueRequest.Builder createQueueRequest = CreateQueueRequest.builder().queueName(configuration.getQueue());

        Map<QueueAttributeName, String> attributes = new HashMap<>();

        if (configuration.getDelaySeconds() != null && configuration.getDelaySeconds() != 0) {
            attributes.put(QueueAttributeName.DELAY_SECONDS, String.valueOf(configuration.getDelaySeconds()));
        }

        if (configuration.getMessageRetentionPeriod() != null && configuration.getMessageRetentionPeriod() != 0) {
            attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, String.valueOf(configuration.getMessageRetentionPeriod()));
        }

        if (configuration.getMaximumMessageSize() != null && configuration.getMaximumMessageSize() != 0) {
            attributes.put(QueueAttributeName.MAXIMUM_MESSAGE_SIZE, String.valueOf(configuration.getMaximumMessageSize()));
        }

        if (configuration.getVisibilityTimeout() != null && configuration.getVisibilityTimeout() != 0) {
            attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(configuration.getVisibilityTimeout()));
        }

        if (configuration.getFifo()) {
            attributes.put(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString());
        }

        if (!attributes.isEmpty()) {
            createQueueRequest.attributes(attributes);
        }

        String queueUrl = client.createQueue(createQueueRequest.build()).queueUrl();

        LOGGER.debug("Queue created (queueUrl={})", queueUrl);

        addQueue(queueUrl);

        return queueUrl;
    }

    /**
     * @param queueName
     * @param receiptHandle
     */
    public void deleteMessage(String queueName, String receiptHandle) {
        String queueUrl = getQueueUrl(queueName);
        client.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(receiptHandle).build());
        LOGGER.debug("Message deleted (queueUrl={}})", queueUrl);
    }

    /**
     * @param queueName
     */
    public void deleteQueue(String queueName) {
        String queueUrl = getQueueUrl(queueName);
        client.deleteQueue(b -> b.queueUrl(queueUrl));
        removeQueue(queueUrl);
    }

    /**
     * @param queueName
     * @return
     */
    public Map<QueueAttributeName, String> getQueueAttributes(String queueName) {
        String queueUrl = getQueueUrl(queueName);

        GetQueueAttributesRequest.Builder getQueueAttributesRequest = GetQueueAttributesRequest
            .builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.ALL);

        try {
            return client.getQueueAttributes(getQueueAttributesRequest.build()).attributes();
        } catch (AwsServiceException exception) {
            if ("AWS.SimpleQueueService.NonExistentQueue".equals(exception.awsErrorDetails().errorCode())) {
                removeQueue(queueUrl);
            }
            LOGGER.warn("An amazon service exception was caught while getting queue attributes", exception);
            return Collections.emptyMap();
        } catch (SdkClientException exception) {
            LOGGER.warn("An amazon client exception was catched while getting queue attributes", exception);
            return Collections.emptyMap();
        }
    }

    /**
     * @return the queue URL
     */
    public String getQueueUrl(String queueName) {
        if (queueName.startsWith("http://") || queueName.startsWith("https://")) {
            return queueName;
        }

        if (StringUtils.isNotEmpty(configuration.getQueueNamePrefix())) {
            queueName = configuration.getQueueNamePrefix() + queueName;
        }

        if (queueUrlByNames.isEmpty() && isCaching()) {
            loadQueues();
        }

        String queueUrl = isCaching() ? queueUrlByNames.get(queueName) : getQueueUrlDirect(queueName);

        if (StringUtils.isEmpty(queueUrl) && (configuration.isAutoCreateQueue())) {
            queueUrl = createQueue(queueName);
        }

        if (StringUtils.isEmpty(queueUrl)) {
            throw QueueDoesNotExistException.builder().message("Queue " + queueName + " not found").build();
        }

        return queueUrl;
    }

    @Override
    public String getQueueArn(String queueName) {
        GetQueueAttributesResponse attributes = client.getQueueAttributes(b -> b.queueUrl(getQueueUrl(queueName)).attributeNames(QueueAttributeName.QUEUE_ARN));
        return attributes.attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    /**
     * @param reload
     * @return
     */
    public List<String> listQueueNames(boolean reload) {
        if (queueUrlByNames.isEmpty() || reload) {
            loadQueues();
        }
        return queueUrlByNames.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * @param reload
     * @return
     */
    public List<String> listQueueUrls(boolean reload) {
        if (queueUrlByNames.isEmpty() || reload) {
            loadQueues();
        }
        return queueUrlByNames.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * @param queueName
     * @param maxNumberOfMessages
     * @param visibilityTimeout
     * @param waitTimeSeconds
     * @return
     */
    public List<Message> receiveMessages(String queueName, int maxNumberOfMessages, int visibilityTimeout, int waitTimeSeconds) {
        String queueUrl = getQueueUrl(queueName);

        ReceiveMessageRequest.Builder receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl);
        if (maxNumberOfMessages > 0) {
            receiveMessageRequest.maxNumberOfMessages(maxNumberOfMessages);
        }
        if (visibilityTimeout > 0) {
            receiveMessageRequest.visibilityTimeout(visibilityTimeout);
        }
        if (waitTimeSeconds > 0) {
            receiveMessageRequest.waitTimeSeconds(waitTimeSeconds);
        }
        List<Message> messages = client.receiveMessage(receiveMessageRequest.build()).messages();
        LOGGER.debug("Messages received (count={})", messages.size());
        return messages;
    }

    /**
     * @param queueName
     * @param messageBody
     * @param delaySeconds
     * @return
     */
    public String sendMessage(String queueName, String messageBody, int delaySeconds, String groupId) {
        String queueUrl = getQueueUrl(queueName);

        SendMessageRequest.Builder request = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody);
        if (delaySeconds > 0) {
            request.delaySeconds(delaySeconds);
        }

        if (StringUtils.isNotEmpty(groupId)) {
            request.messageGroupId(groupId);
        }

        String messageId = client.sendMessage(request.build()).messageId();
        LOGGER.debug("Message sent (messageId={})", messageId);
        return messageId;
    }

    /**
     * @param queueName
     * @param messageBody
     * @return
     */
    public String sendMessage(String queueName, String messageBody, Consumer<SendMessageRequest.Builder> messageConfiguration) {
        String queueUrl = getQueueUrl(queueName);

        SendMessageRequest.Builder request = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody);

        messageConfiguration.accept(request);

        String messageId = client.sendMessage(request.build()).messageId();
        LOGGER.debug("Message sent (messageId={})", messageId);
        return messageId;
    }

    void assertDefaultQueueName() {
        if (StringUtils.isEmpty(configuration.getQueue())) {
            throw new IllegalStateException("Queue not configured");
        }
    }

    // PRIVATE
    private static String getQueueNameFromUrl(String queueUrl) {
        return Arrays.stream(queueUrl.split("/")).reduce((first, second) -> second).orElse(null);
    }

    private String getQueueUrlDirect(String queueName) {
        try {
            return client.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
        } catch (QueueDoesNotExistException ignored) {
            return null;
        }
    }

    private void addQueue(String queueUrl) {
        if (StringUtils.isEmpty(queueUrl)) {
            throw new IllegalStateException("Queue URL cannot be null or empty");
        }

        synchronized (queueUrlByNames) {
            queueUrlByNames.put(getQueueNameFromUrl(queueUrl),  queueUrl);
        }
    }

    private void loadQueues() {
        ListQueuesRequest.Builder listQueuesRequest = ListQueuesRequest.builder();
        if (StringUtils.isNotEmpty(configuration.getQueueNamePrefix())) {
            listQueuesRequest.queueNamePrefix(configuration.getQueueNamePrefix());
        }

        Map<String, String> queueUrls = client.listQueues(listQueuesRequest.build())
            .queueUrls()
            .stream()
            .collect(
                Collectors.toMap(
                    DefaultSimpleQueueService::getQueueNameFromUrl,
                    Function.identity()
                )
            );

        synchronized (queueUrlByNames) {
            queueUrlByNames.clear();
            queueUrlByNames.putAll(queueUrls);
        }
    }

    private void removeQueue(String queueUrl) {
        if (StringUtils.isEmpty(queueUrl)) {
            throw new IllegalStateException("Queue URL cannot be null or empty");
        }

        synchronized (queueUrlByNames) {
            queueUrlByNames.remove(getQueueNameFromUrl(queueUrl));
        }
    }

}
