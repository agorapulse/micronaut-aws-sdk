package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.s3;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageServiceConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageServiceFactory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = SimpleStorageServiceFactory.class, beans = LocalstackContainerHolder.class)
public class SimpleStorageServiceConfigurationListener implements BeanCreatedEventListener<SimpleStorageServiceConfiguration> {

    private final LocalstackContainerHolder holder;

    public SimpleStorageServiceConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.S3);
    }

    @Override
    public SimpleStorageServiceConfiguration onCreated(BeanCreatedEvent<SimpleStorageServiceConfiguration> event) {
        SimpleStorageServiceConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        return conf;
    }

}
