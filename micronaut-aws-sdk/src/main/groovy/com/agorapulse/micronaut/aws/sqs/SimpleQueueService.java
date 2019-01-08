package com.agorapulse.micronaut.aws.sqs;

import com.amazonaws.services.sqs.model.Message;

import java.util.List;
import java.util.Map;

/**
 * Amazon SQS services
 *
 * @see SimpleQueueServiceConfiguration
 */
public interface SimpleQueueService {

    /**
     * @return the default queue name from the configuration
     * @throws AssertionError if the default queue name is not set (aws.sqs.queue configuration value)
     */
    String getDefaultQueueName();

    /**
     * @return true if the service is caching queues urls, useful for longer running applications
     */
    boolean isCaching();

    String createQueue(QueueConfiguration queueConfiguration);

    /**
     * Creates new queue with given name.
     * @param queueName the queue name
     * @return url of the new queue
     */
    String createQueue(String queueName);

    /**
     * Deletes message from the queue.
     * @param queueName the queue name
     * @param receiptHandle message id
     */
    void deleteMessage(String queueName, String receiptHandle);

    /**
     * Deletes message from the default queue.
     * @param receiptHandle message id
     */

    default void deleteMessage(String receiptHandle) {
        deleteMessage(getDefaultQueueName(), receiptHandle);
    }

    /**
     * Deletes queue by name.
     * @param queueName the name of the queue
     */
    void deleteQueue(String queueName);

    /**
     * @param queueName the name of the queue
     * @return the attributes of the queue
     */
    Map<String, String> getQueueAttributes(String queueName);

    /**
     * @return the attributes of the default queue
     */
    default Map<String, String> getQueueAttributes() {
        return getQueueAttributes(getDefaultQueueName());
    }

    /**
     * @param queueName the queue name
     * @return the queue url
     */
    String getQueueUrl(String queueName);

    /**
     * @return the default queue url
     */
    default String getQueueUrl() {
        return getQueueUrl(getDefaultQueueName());
    }

    /**
     * @param queueName the queue name
     * @return the queue arn
     */
    String getQueueArn(String queueName);

    /**
     * @return the default queue arn
     */
    default String getQueueArn() {
        return getQueueArn(getDefaultQueueName());
    }

    /**
     * @return list of all queues' names
     */
    default List<String> listQueueNames() {
        return listQueueNames(!isCaching());
    }

    /**
     * @param reload whether to reload the list of the queue
     * @return the list of all queues' names
     */
    List<String> listQueueNames(boolean reload);

    /**
     * @return the list of all queues' urls
     */
    default List<String> listQueueUrls() {
        return listQueueUrls(!isCaching());
    }

    /**
     * @param reload whether to reload the list of the queue
     * @return the list of all queues' urls
     */
    List<String> listQueueUrls(boolean reload);

    /**
     * @param queueName the queue name
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @param visibilityTimeout the visibility timeout of the messages
     * @return the list of received messages from the given queue
     */
    default List<Message> receiveMessages(String queueName, int maxNumberOfMessages, int visibilityTimeout) {
        return receiveMessages(queueName, maxNumberOfMessages, visibilityTimeout, 0);
    }

    /**
     * @param queueName the queue name
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @return the list of received messages from the given queue
     */
    default List<Message> receiveMessages(String queueName, int maxNumberOfMessages) {
        return receiveMessages(queueName, maxNumberOfMessages, 0);
    }

    /**
     * @param queueName the queue name
     * @return the list of received messages from the given queue
     */
    default List<Message> receiveMessages(String queueName) {
        return receiveMessages(queueName, 1);
    }

    /**
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @param visibilityTimeout the visibility timeout of the messages
     * @param waitTimeSeconds the time to wait for the messages to arrive
     * @return the list of received messages from the default queue
     */
    default List<Message> receiveMessages(int maxNumberOfMessages, int visibilityTimeout, int waitTimeSeconds) {
        return receiveMessages(getDefaultQueueName(), maxNumberOfMessages, visibilityTimeout, waitTimeSeconds);
    }

    /**
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @param visibilityTimeout the visibility timeout of the messages
     * @return the list of received messages from the default queue
     */
    default List<Message> receiveMessages(int maxNumberOfMessages, int visibilityTimeout) {
        return receiveMessages(getDefaultQueueName(), maxNumberOfMessages, visibilityTimeout, 0);
    }

    /**
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @return the list of received messages from the default queue
     */
    default List<Message> receiveMessages(int maxNumberOfMessages) {
        return receiveMessages(getDefaultQueueName(), maxNumberOfMessages, 0);
    }

    /**
     * @return the list of received messages from the default queue
     */
    default List<Message> receiveMessages() {
        return receiveMessages(getDefaultQueueName(), 1);
    }

    /**
     * @param queueName the queue name
     * @param maxNumberOfMessages the maximum number of messages to retrieve
     * @param visibilityTimeout the visibility timeout of the messages
     * @param waitTimeSeconds the time to wait for the messages to arrive
     * @return the list of received messages from the given queue
     */
    List<Message> receiveMessages(String queueName, int maxNumberOfMessages, int visibilityTimeout, int waitTimeSeconds);

    /**
     * Send message immediately
     * @param queueName the name of the queue
     * @param messageBody the body of the message
     * @return the message id of the message sent
     */
    default String sendMessage(String queueName, String messageBody) {
        return sendMessage(queueName, messageBody, 0);
    }

    /**
     * Send message with given delay.
     * @param queueName the name of the queue
     * @param messageBody the body of the message
     * @param delaySeconds the delay in seconds
     * @param groupId group id for FIFO queues
     * @return the message id of the message sent
     */
    String sendMessage(String queueName, String messageBody, int delaySeconds, String groupId);

    /**
     * Send message with given delay.
     * @param queueName the name of the queue
     * @param messageBody the body of the message
     * @param delaySeconds the delay in seconds
     * @return the message id of the message sent
     */
    default String sendMessage(String queueName, String messageBody, int delaySeconds) {
        return sendMessage(queueName, messageBody, delaySeconds, null);
    }

    /**
     * Send message in default queue immediately
     * @param messageBody the body of the message
     * @return the message id of the message sent
     */
    default String sendMessage(String messageBody) {
        return sendMessage(getDefaultQueueName(), messageBody);
    }

    /**
     * Send message in the default queue with given delay.
     * @param messageBody the body of the message
     * @param delaySeconds the delay in seconds
     * @return the message id of the message sent
     */
    default String sendMessage(String messageBody, int delaySeconds) {
        return sendMessage(getDefaultQueueName(), messageBody, delaySeconds);
    }

    /**
     * Send message with given delay.
     * @param messageBody the body of the message
     * @param delaySeconds the delay in seconds
     * @param groupId group id for FIFO queues
     * @return the message id of the message sent
     */
    default String sendMessage(String messageBody, int delaySeconds, String groupId) {
        return sendMessage(getDefaultQueueName(), messageBody, delaySeconds, null);
    }

}
