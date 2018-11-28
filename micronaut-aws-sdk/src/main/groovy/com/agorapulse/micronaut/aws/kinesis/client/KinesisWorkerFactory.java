package com.agorapulse.micronaut.aws.kinesis.client;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;

public interface KinesisWorkerFactory {

    KinesisWorker create(KinesisClientLibConfiguration configuration);

}
