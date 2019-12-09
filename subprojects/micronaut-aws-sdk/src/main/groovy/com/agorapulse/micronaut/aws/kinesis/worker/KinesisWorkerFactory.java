package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;

public interface KinesisWorkerFactory {

    KinesisWorker create(KinesisClientLibConfiguration configuration);

}
