package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.cloudwatch;

import com.agorapulse.micronaut.amazon.awssdk.cloudwatch.CloudWatchConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = CloudWatchConfiguration.class, beans = LocalstackContainerHolder.class)
public class CloudWatchConfigurationListener implements BeanCreatedEventListener<CloudWatchConfiguration> {

    private final LocalstackContainerHolder holder;

    public CloudWatchConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.CLOUDWATCH);
    }

    @Override
    public CloudWatchConfiguration onCreated(BeanCreatedEvent<CloudWatchConfiguration> event) {
        CloudWatchConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCH).toString());
        return conf;
    }

}
