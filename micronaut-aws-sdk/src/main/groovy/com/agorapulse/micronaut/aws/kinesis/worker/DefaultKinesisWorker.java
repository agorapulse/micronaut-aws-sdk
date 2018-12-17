package com.agorapulse.micronaut.aws.kinesis.worker;

import com.agorapulse.micronaut.aws.kinesis.annotation.KinesisListener;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.Record;
import io.micronaut.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

class DefaultKinesisWorker implements KinesisWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisListener.class);

    DefaultKinesisWorker(KinesisClientLibConfiguration configuration, ExecutorService executorService, Optional<AmazonDynamoDB> amazonDynamoDB, Optional<AmazonKinesis> amazonKinesis) {
        this.configuration = configuration;
        this.executorService = executorService;
        this.amazonDynamoDB = amazonDynamoDB;
        this.amazonKinesis = amazonKinesis;
    }

    @Override
    public void start() {
        Worker.Builder builder = new Worker.Builder().config(configuration);

        if (StringUtils.isEmpty(configuration.getDynamoDBEndpoint())) {
            amazonDynamoDB.ifPresent(builder::dynamoDBClient);
        }

        if (StringUtils.isEmpty(configuration.getKinesisEndpoint())) {
            amazonKinesis.ifPresent(builder::kinesisClient);
        }

        builder.recordProcessorFactory(DefaultRecordProcessorFactory.create((string, record) ->
            consumers.forEach(c -> {
                try {
                    c.accept(string, record);
                } catch (Exception e) {
                    LOGGER.error("Exception processing Kinesis record " + record + " decoded as " + string, e);
                }
            }))
        );

        try {
            LOGGER.info("Starting Kinesis worker for {}", configuration.getStreamName());
            executorService.execute(builder.build());
        } catch (Exception t) {
            LOGGER.error("Caught throwable while processing Kinesis data.", t);
        }
    }

    @Override
    public void addConsumer(BiConsumer<String, Record> next) {
        consumers.add(next);
    }

    private final KinesisClientLibConfiguration configuration;
    private final ExecutorService executorService;
    private final Optional<AmazonDynamoDB> amazonDynamoDB;
    private final Optional<AmazonKinesis> amazonKinesis;
    private final List<BiConsumer<String, Record>> consumers = new CopyOnWriteArrayList<>();
}
