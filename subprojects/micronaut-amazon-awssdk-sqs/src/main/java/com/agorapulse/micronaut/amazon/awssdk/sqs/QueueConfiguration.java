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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class QueueConfiguration implements Cloneable {
    public QueueConfiguration withQueue(String queue) {
        this.queue = queue;
        return this;
    }

    public QueueConfiguration withFifo(boolean fifo) {
        this.fifo = fifo;
        return this;
    }

    public QueueConfiguration withDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
        return this;
    }

    public QueueConfiguration withMessageRetentionPeriod(Integer messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
        return this;
    }

    public QueueConfiguration withMaximumMessageSize(Integer maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
        return this;
    }

    public QueueConfiguration withVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
        return this;
    }

    public QueueConfiguration copy() {
        return new QueueConfiguration()
            .withQueue(queue)
            .withFifo(fifo)
            .withDelaySeconds(delaySeconds)
            .withMessageRetentionPeriod(messageRetentionPeriod)
            .withMaximumMessageSize(maximumMessageSize)
            .withVisibilityTimeout(visibilityTimeout);
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public boolean getFifo() {
        return fifo;
    }

    public boolean isFifo() {
        return fifo;
    }

    public void setFifo(boolean fifo) {
        this.fifo = fifo;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public Integer getMessageRetentionPeriod() {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(Integer messageRetentionPeriod) {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    public Integer getMaximumMessageSize() {
        return maximumMessageSize;
    }

    public void setMaximumMessageSize(Integer maximumMessageSize) {
        this.maximumMessageSize = maximumMessageSize;
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    private String queue = "";
    private boolean fifo;

    @Min(0L)
    @Max(900L)
    private Integer delaySeconds = 0;

    @Min(60L)
    @Max(1209600L)
    private Integer messageRetentionPeriod = 345600;

    @Min(1024L)
    @Max(262144L)
    private Integer maximumMessageSize = 262144;

    @Min(0L)
    @Max(43200L)
    private Integer visibilityTimeout = 30;
}
