package com.agorapulse.micronaut.aws.kinesis.client

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.model.Record
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import javax.annotation.PostConstruct
import java.util.concurrent.ExecutorService
import java.util.function.BiConsumer

@Slf4j
@CompileStatic
class KinesisClient {

    private final BiConsumer<String, Record> consumer
    private final KinesisClientLibConfiguration configuration
    private final ExecutorService executorService

    KinesisClient(KinesisClientLibConfiguration configuration, ExecutorService executorService, BiConsumer<String, Record> consumer) {
        this.configuration = configuration
        this.executorService = executorService
        this.consumer = consumer
    }

    @PostConstruct
    void init() {
        Worker worker = new Worker(DefaultRecordProcessorFactory.create(consumer), configuration)
        try {
            log.info "Starting Kinesis worker for ${configuration.streamName}"
            executorService.execute(worker)
        } catch (Throwable t) {
            log.error "Caught throwable while processing Kinesis data.", t
        }
    }

}
