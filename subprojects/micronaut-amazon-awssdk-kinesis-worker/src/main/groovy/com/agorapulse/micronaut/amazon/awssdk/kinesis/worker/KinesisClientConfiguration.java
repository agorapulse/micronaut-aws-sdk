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
package com.agorapulse.micronaut.amazon.awssdk.kinesis.worker;

import io.micronaut.core.util.StringUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import javax.annotation.Nonnull;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.Optional;

/**
 * Default configuration for Kinesis listener.
 */
public abstract class KinesisClientConfiguration {

    private static final long DEFAULT_FAILOVER_TIME_MILLIS = 10000L;
    private static final long DEFAULT_SHARD_SYNC_INTERVAL_MILLIS = 1000L;
    private static final long DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS = 10000L;
    private static final boolean DEFAULT_CLEANUP_LEASES_UPON_SHARDS_COMPLETION = true;
    private static final long DEFAULT_TASK_BACKOFF_TIME_MILLIS = 500L;
    private static final int DEFAULT_MAX_LEASES_FOR_WORKER = Integer.MAX_VALUE;
    private static final int DEFAULT_MAX_LEASES_TO_STEAL_AT_ONE_TIME = 1;
    private static final int DEFAULT_INITIAL_LEASE_TABLE_READ_CAPACITY = 10;
    private static final int DEFAULT_INITIAL_LEASE_TABLE_WRITE_CAPACITY = 10;
    private static final int DEFAULT_MAX_LIST_SHARDS_RETRY_ATTEMPTS = 50;

    @NotEmpty private final String applicationName;
    @NotEmpty private final String workerId;
    @NotEmpty private String stream;

    private String tableName;
    private String kinesisEndpoint;
    private String dynamoDBEndpoint;

    private boolean callProcessRecordsEvenForEmptyRecordList;
    private boolean ignoreUnexpectedChildShards;
    private boolean cleanupLeasesUponShardCompletion = DEFAULT_CLEANUP_LEASES_UPON_SHARDS_COMPLETION;
    private boolean skipShardSyncAtWorkerInitializationIfLeasesExist;

    @Min(1L) private long failoverTimeMillis = DEFAULT_FAILOVER_TIME_MILLIS;
    @Min(1L) private long shardSyncIntervalMillis = DEFAULT_SHARD_SYNC_INTERVAL_MILLIS;
    @Min(1L) private long parentShardPollIntervalMillis = DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS;
    @Min(1L) private long taskBackoffTimeMillis = DEFAULT_TASK_BACKOFF_TIME_MILLIS;

    private int maxLeasesForWorker = DEFAULT_MAX_LEASES_FOR_WORKER;
    private int maxLeasesToStealAtOneTime = DEFAULT_MAX_LEASES_TO_STEAL_AT_ONE_TIME;
    private int initialLeaseTableReadCapacity = DEFAULT_INITIAL_LEASE_TABLE_READ_CAPACITY;
    private int initialLeaseTableWriteCapacity = DEFAULT_INITIAL_LEASE_TABLE_WRITE_CAPACITY;
    private int maxListShardsRetryAttempts = DEFAULT_MAX_LIST_SHARDS_RETRY_ATTEMPTS;

    private Optional<Long> logWarningForTaskAfterMillis = Optional.empty();

    protected KinesisClientConfiguration(@Nonnull String applicationName, @Nonnull  String workerId) {
        this.applicationName = applicationName;
        this.workerId = workerId;
    }

    protected ConfigsBuilder getConfigsBuilder(
        Optional<DynamoDbAsyncClient> amazonDynamoDB,
        Optional<KinesisAsyncClient> amazonKinesis,
        Optional<CloudWatchAsyncClient> amazonCloudWatch,
        ShardRecordProcessorFactory recordProcessorFactory
    ) {
        ConfigsBuilder builder = new ConfigsBuilder(
            stream,
            applicationName,
            StringUtils.isEmpty(kinesisEndpoint) ? amazonKinesis.orElseGet(() -> KinesisAsyncClient.builder().build()) : KinesisAsyncClient.builder().endpointOverride(URI.create(kinesisEndpoint)).build(),
            StringUtils.isEmpty(dynamoDBEndpoint) ? amazonDynamoDB.orElseGet(() -> DynamoDbAsyncClient.builder().build()) : DynamoDbAsyncClient.builder().endpointOverride(URI.create(dynamoDBEndpoint)).build(),
            amazonCloudWatch.orElseGet(() -> CloudWatchAsyncClient.builder().build()),
            workerId,
            recordProcessorFactory
        );

        if (StringUtils.isNotEmpty(tableName)) {
            builder.tableName(tableName);
        }

        if (failoverTimeMillis != DEFAULT_FAILOVER_TIME_MILLIS) {
            builder.leaseManagementConfig().failoverTimeMillis(failoverTimeMillis);
        }

        if (shardSyncIntervalMillis != DEFAULT_SHARD_SYNC_INTERVAL_MILLIS) {
            builder.coordinatorConfig().shardConsumerDispatchPollIntervalMillis(shardSyncIntervalMillis);
        }

        if (callProcessRecordsEvenForEmptyRecordList) {
            builder.processorConfig().callProcessRecordsEvenForEmptyRecordList(true);
        }

        if (parentShardPollIntervalMillis != DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS) {
            builder.coordinatorConfig().parentShardPollIntervalMillis(parentShardPollIntervalMillis);
        }

        if (!cleanupLeasesUponShardCompletion) {
            builder.leaseManagementConfig().cleanupLeasesUponShardCompletion(false);
        }

        if (ignoreUnexpectedChildShards) {
            builder.leaseManagementConfig().ignoreUnexpectedChildShards(true);
        }

        if (taskBackoffTimeMillis != DEFAULT_TASK_BACKOFF_TIME_MILLIS) {
            builder.lifecycleConfig().taskBackoffTimeMillis(taskBackoffTimeMillis);
        }

        if (maxLeasesForWorker != DEFAULT_MAX_LEASES_FOR_WORKER) {
            builder.leaseManagementConfig().maxLeasesForWorker(maxLeasesForWorker);
        }

        if (maxLeasesToStealAtOneTime != DEFAULT_MAX_LEASES_TO_STEAL_AT_ONE_TIME) {
            builder.leaseManagementConfig().maxLeasesToStealAtOneTime(maxLeasesToStealAtOneTime);
        }

        if (initialLeaseTableReadCapacity != DEFAULT_INITIAL_LEASE_TABLE_READ_CAPACITY) {
            builder.leaseManagementConfig().initialLeaseTableReadCapacity(initialLeaseTableReadCapacity);
        }

        if (initialLeaseTableWriteCapacity != DEFAULT_INITIAL_LEASE_TABLE_WRITE_CAPACITY) {
            builder.leaseManagementConfig().initialLeaseTableWriteCapacity(initialLeaseTableWriteCapacity);
        }

        if (skipShardSyncAtWorkerInitializationIfLeasesExist) {
            builder.coordinatorConfig().skipShardSyncAtWorkerInitializationIfLeasesExist(true);
        }

        if (logWarningForTaskAfterMillis.isPresent()) {
            builder.lifecycleConfig().logWarningForTaskAfterMillis(logWarningForTaskAfterMillis);
        }

        if (maxListShardsRetryAttempts != DEFAULT_MAX_LIST_SHARDS_RETRY_ATTEMPTS) {
            builder.retrievalConfig().maxListShardsRetryAttempts(maxListShardsRetryAttempts);
        }

        return builder;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setKinesisEndpoint(String kinesisEndpoint) {
        this.kinesisEndpoint = kinesisEndpoint;
    }

    public void setDynamoDBEndpoint(String dynamoDBEndpoint) {
        this.dynamoDBEndpoint = dynamoDBEndpoint;
    }

    public void setCallProcessRecordsEvenForEmptyRecordList(boolean callProcessRecordsEvenForEmptyRecordList) {
        this.callProcessRecordsEvenForEmptyRecordList = callProcessRecordsEvenForEmptyRecordList;
    }

    public void setIgnoreUnexpectedChildShards(boolean ignoreUnexpectedChildShards) {
        this.ignoreUnexpectedChildShards = ignoreUnexpectedChildShards;
    }

    public void setCleanupLeasesUponShardCompletion(boolean cleanupLeasesUponShardCompletion) {
        this.cleanupLeasesUponShardCompletion = cleanupLeasesUponShardCompletion;
    }

    public void setSkipShardSyncAtWorkerInitializationIfLeasesExist(boolean skipShardSyncAtWorkerInitializationIfLeasesExist) {
        this.skipShardSyncAtWorkerInitializationIfLeasesExist = skipShardSyncAtWorkerInitializationIfLeasesExist;
    }

    public void setFailoverTimeMillis(long failoverTimeMillis) {
        this.failoverTimeMillis = failoverTimeMillis;
    }

    public void setShardSyncIntervalMillis(long shardSyncIntervalMillis) {
        this.shardSyncIntervalMillis = shardSyncIntervalMillis;
    }

    public void setParentShardPollIntervalMillis(long parentShardPollIntervalMillis) {
        this.parentShardPollIntervalMillis = parentShardPollIntervalMillis;
    }

    public void setTaskBackoffTimeMillis(long taskBackoffTimeMillis) {
        this.taskBackoffTimeMillis = taskBackoffTimeMillis;
    }

    public void setMaxLeasesForWorker(int maxLeasesForWorker) {
        this.maxLeasesForWorker = maxLeasesForWorker;
    }

    public void setMaxLeasesToStealAtOneTime(int maxLeasesToStealAtOneTime) {
        this.maxLeasesToStealAtOneTime = maxLeasesToStealAtOneTime;
    }

    public void setInitialLeaseTableReadCapacity(int initialLeaseTableReadCapacity) {
        this.initialLeaseTableReadCapacity = initialLeaseTableReadCapacity;
    }

    public void setInitialLeaseTableWriteCapacity(int initialLeaseTableWriteCapacity) {
        this.initialLeaseTableWriteCapacity = initialLeaseTableWriteCapacity;
    }

    public void setMaxListShardsRetryAttempts(int maxListShardsRetryAttempts) {
        this.maxListShardsRetryAttempts = maxListShardsRetryAttempts;
    }

    public void setLogWarningForTaskAfterMillis(Optional<Long> logWarningForTaskAfterMillis) {
        this.logWarningForTaskAfterMillis = logWarningForTaskAfterMillis;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getStream() {
        return stream;
    }
}
