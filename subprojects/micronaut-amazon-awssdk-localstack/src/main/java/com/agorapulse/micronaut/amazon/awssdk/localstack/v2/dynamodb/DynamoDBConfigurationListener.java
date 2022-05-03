package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBClientsFactory;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = DynamoDBClientsFactory.class, beans = LocalstackContainerHolder.class)
public class DynamoDBConfigurationListener implements BeanCreatedEventListener<DynamoDBConfiguration> {

    private final LocalstackContainerHolder holder;

    public DynamoDBConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.DYNAMODB);
    }

    @Override
    public DynamoDBConfiguration onCreated(BeanCreatedEvent<DynamoDBConfiguration> event) {
        DynamoDBConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
        return conf;
    }

}
