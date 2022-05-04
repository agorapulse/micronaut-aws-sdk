package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.sqs;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.sqs.SimpleQueueServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = SimpleQueueServiceConfiguration.class, beans = LocalstackContainerHolder.class)
public class SimpleQueueServiceConfigurationListener implements BeanCreatedEventListener<SimpleQueueServiceConfiguration> {

    private final LocalstackContainerHolder holder;

    public SimpleQueueServiceConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.SQS);
    }

    @Override
    public SimpleQueueServiceConfiguration onCreated(BeanCreatedEvent<SimpleQueueServiceConfiguration> event) {
        SimpleQueueServiceConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        return conf;
    }

}
