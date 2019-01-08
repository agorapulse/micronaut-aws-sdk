package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.model.Record;

import java.util.function.BiConsumer;

public interface KinesisWorker {
    void start();
    void shutdown();
    void addConsumer(BiConsumer<String, Record> next);
}
