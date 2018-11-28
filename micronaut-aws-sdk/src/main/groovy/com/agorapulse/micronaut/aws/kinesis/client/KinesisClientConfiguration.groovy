package com.agorapulse.micronaut.aws.kinesis.client

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.SimpleRecordsFetcherFactory
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value

import javax.inject.Named
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty

import static com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration.*

@CompileStatic
@Named('default')
@ConfigurationProperties('aws.kinesis.client')
@Requires(classes = KinesisClientLibConfiguration)
class KinesisClientConfiguration {

    @NotEmpty
    String applicationName

    @NotEmpty
    String workerId

    @NotEmpty
    String stream

    String tableName = '';
    String kinesisEndpoint;
    String dynamoDBEndpoint;
    InitialPositionInStream initialPositionInStream = DEFAULT_INITIAL_POSITION_IN_STREAM;
    @Min(1L)
    long failoverTimeMillis = DEFAULT_FAILOVER_TIME_MILLIS;
    @Min(1L)
    long shardSyncIntervalMillis = DEFAULT_SHARD_SYNC_INTERVAL_MILLIS;
    @Min(1L)
    int maxRecords = DEFAULT_MAX_RECORDS;
    @Min(1L)
    long idleTimeBetweenReadsInMillis = DEFAULT_IDLETIME_BETWEEN_READS_MILLIS;
    boolean callProcessRecordsEvenForEmptyRecordList;
    @Min(1L)
    long parentShardPollIntervalMillis = DEFAULT_PARENT_SHARD_POLL_INTERVAL_MILLIS;
    boolean cleanupLeasesUponShardCompletion = DEFAULT_CLEANUP_LEASES_UPON_SHARDS_COMPLETION;
    boolean ignoreUnexpectedChildShards;
    @Min(1L)
    long taskBackoffTimeMillis = DEFAULT_TASK_BACKOFF_TIME_MILLIS;
    @Min(1L)
    long metricsBufferTimeMillis = DEFAULT_METRICS_BUFFER_TIME_MILLIS;
    @Min(1L)
    int metricsMaxQueueSize = DEFAULT_METRICS_MAX_QUEUE_SIZE;
    MetricsLevel metricsLevel = DEFAULT_METRICS_LEVEL;
    Set<String> metricsEnabledDimensions = DEFAULT_METRICS_ENABLED_DIMENSIONS;
    boolean validateSequenceNumberBeforeCheckpointing = DEFAULT_VALIDATE_SEQUENCE_NUMBER_BEFORE_CHECKPOINTING;
    int maxLeasesForWorker = DEFAULT_MAX_LEASES_FOR_WORKER;
    int maxLeasesToStealAtOneTime = DEFAULT_MAX_LEASES_TO_STEAL_AT_ONE_TIME;
    int initialLeaseTableReadCapacity = DEFAULT_INITIAL_LEASE_TABLE_READ_CAPACITY;
    int initialLeaseTableWriteCapacity = DEFAULT_INITIAL_LEASE_TABLE_WRITE_CAPACITY;
    boolean skipShardSyncAtWorkerInitializationIfLeasesExist = DEFAULT_SKIP_SHARD_SYNC_AT_STARTUP_IF_LEASES_EXIST;
    @Min(1L)
    long shutdownGraceMillis = DEFAULT_SHUTDOWN_GRACE_MILLIS;
    int timeoutInSeconds = 0
    int retryGetRecordsInSeconds = 0
    int maxGetRecordsThreadPool = 0
    long logWarningForTaskAfterMillis = 0
    int maxLeaseRenewalThreads = DEFAULT_MAX_LEASE_RENEWAL_THREADS;
    long listShardsBackoffTimeInMillis = DEFAULT_LIST_SHARDS_BACKOFF_TIME_IN_MILLIS;
    int maxListShardsRetryAttempts = DEFAULT_MAX_LIST_SHARDS_RETRY_ATTEMPTS;

    KinesisClientConfiguration(
        @Value('${aws.kinesis.application.name:}') String applicationName,
        @Value('${aws.kinesis.worker.id:}') String workerId
    ) {
        this.applicationName = applicationName
        if (workerId) {
            this.workerId = workerId
        } else {
            this.workerId = "${InetAddress.localHost.canonicalHostName}:${UUID.randomUUID()}"
        }
    }

    KinesisClientLibConfiguration getKinesisClientLibConfiguration(ClientConfiguration awsClientConfiguration, AWSCredentialsProvider credentialsProvider, String region) {
        KinesisClientLibConfiguration configuration = new KinesisClientLibConfiguration(
            applicationName,
            stream,
            kinesisEndpoint,
            dynamoDBEndpoint,
            initialPositionInStream,
            credentialsProvider,
            credentialsProvider,
            credentialsProvider,
            failoverTimeMillis,
            workerId,
            maxRecords,
            idleTimeBetweenReadsInMillis,
            callProcessRecordsEvenForEmptyRecordList,
            parentShardPollIntervalMillis,
            shardSyncIntervalMillis,
            cleanupLeasesUponShardCompletion,
            awsClientConfiguration,
            awsClientConfiguration,
            awsClientConfiguration,
            taskBackoffTimeMillis,
            metricsBufferTimeMillis,
            metricsMaxQueueSize,
            validateSequenceNumberBeforeCheckpointing,
            region,
            new SimpleRecordsFetcherFactory()
        )
        configuration
            .withIgnoreUnexpectedChildShards(ignoreUnexpectedChildShards)
            .withMetricsLevel(metricsLevel)
            .withMetricsEnabledDimensions(metricsEnabledDimensions)
            .withValidateSequenceNumberBeforeCheckpointing(validateSequenceNumberBeforeCheckpointing)
            .withMaxLeasesForWorker(maxLeasesForWorker)
            .withMaxLeasesToStealAtOneTime(maxLeasesToStealAtOneTime)
            .withInitialLeaseTableReadCapacity(initialLeaseTableReadCapacity)
            .withInitialLeaseTableWriteCapacity(initialLeaseTableWriteCapacity)
            .withSkipShardSyncAtStartupIfLeasesExist(skipShardSyncAtWorkerInitializationIfLeasesExist)
            .withShutdownGraceMillis(shutdownGraceMillis)
            .withMaxLeaseRenewalThreads(maxLeaseRenewalThreads)
            .withListShardsBackoffTimeInMillis(listShardsBackoffTimeInMillis)
            .withMaxListShardsRetryAttempts(maxListShardsRetryAttempts)

        if (tableName) {
            configuration.withTableName(tableName)
        }

        if (timeoutInSeconds) {
            configuration.withTimeoutInSeconds(timeoutInSeconds)
        }
        if (retryGetRecordsInSeconds) {
            configuration.withRetryGetRecordsInSeconds(retryGetRecordsInSeconds)
        }
        if (maxGetRecordsThreadPool) {
            configuration.withMaxGetRecordsThreadPool(maxGetRecordsThreadPool)
        }
        if (logWarningForTaskAfterMillis) {
            configuration.withLogWarningForTaskAfterMillis(logWarningForTaskAfterMillis)
        }

        return configuration

    }
}
