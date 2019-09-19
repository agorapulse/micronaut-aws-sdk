package com.agorapulse.micronaut.aws.kinesis.worker

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value

import javax.inject.Named

/**
 * Default configuration for Kinesis listener.
 */
@CompileStatic
@Named('default')
@ConfigurationProperties('aws.kinesis.listener')
@Requires(classes = KinesisClientLibConfiguration)
@SuppressWarnings('NoWildcardImports')
class DefaultKinesisClientConfiguration extends KinesisClientConfiguration {

    DefaultKinesisClientConfiguration(
        @Value('${aws.kinesis.application.name:}') String applicationName,
        @Value('${aws.kinesis.worker.id:}') String workerId
    ) {
        super(applicationName, workerId)
    }
}
