package com.agorapulse.micronaut.aws.kinesis.client

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory
import com.amazonaws.services.kinesis.model.Record
import groovy.transform.CompileStatic

import java.util.function.BiConsumer

@CompileStatic
class DefaultRecordProcessorFactory implements IRecordProcessorFactory {

    static IRecordProcessorFactory create(BiConsumer<String, Record> consumer) {
        return new DefaultRecordProcessorFactory(consumer)
    }

    private BiConsumer<String, Record> consumer

    private DefaultRecordProcessorFactory(BiConsumer<String, Record> consumer) {
        this.consumer = consumer
    }

    @Override
    IRecordProcessor createProcessor() {
        return DefaultRecordProcessor.create(consumer)
    }
}
