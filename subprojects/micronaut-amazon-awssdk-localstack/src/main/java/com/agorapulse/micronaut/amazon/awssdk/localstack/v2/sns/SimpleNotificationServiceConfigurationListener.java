package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.sns;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageServiceConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.s3.SimpleStorageServiceFactory;
import com.agorapulse.micronaut.amazon.awssdk.sns.SimpleNotificationServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = SimpleNotificationServiceConfiguration.class, beans = LocalstackContainerHolder.class)
public class SimpleNotificationServiceConfigurationListener implements BeanCreatedEventListener<SimpleNotificationServiceConfiguration> {

    private final LocalstackContainerHolder holder;

    public SimpleNotificationServiceConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.SNS);
    }

    @Override
    public SimpleNotificationServiceConfiguration onCreated(BeanCreatedEvent<SimpleNotificationServiceConfiguration> event) {
        SimpleNotificationServiceConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.SNS).toString());
        return conf;
    }

}
