package com.agorapulse.micronaut.aws.kinesis.worker;

import com.amazonaws.services.kinesis.model.Record;

import java.util.function.BiConsumer;

public interface KinesisWorker {
    void start();
    void addConsumer(BiConsumer<String, Record> next);
}
