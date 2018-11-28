package com.agorapulse.micronaut.aws.kinesis.client;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.model.Record;

import java.util.function.BiConsumer;

class DefaultRecordProcessorFactory implements IRecordProcessorFactory {

    static IRecordProcessorFactory create(BiConsumer<String, Record> consumer) {
        return new DefaultRecordProcessorFactory(consumer);
    }

    private DefaultRecordProcessorFactory(BiConsumer<String, Record> consumer) {
        this.consumer = consumer;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return DefaultRecordProcessor.create(consumer);
    }

    private BiConsumer<String, Record> consumer;

}
