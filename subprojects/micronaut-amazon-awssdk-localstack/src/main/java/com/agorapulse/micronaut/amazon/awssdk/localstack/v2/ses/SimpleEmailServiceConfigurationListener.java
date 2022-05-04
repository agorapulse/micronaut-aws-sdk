package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.ses;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.ses.SimpleEmailServiceConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.sqs.SimpleQueueServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = SimpleEmailServiceConfiguration.class, beans = LocalstackContainerHolder.class)
public class SimpleEmailServiceConfigurationListener implements BeanCreatedEventListener<SimpleEmailServiceConfiguration> {

    private final LocalstackContainerHolder holder;

    public SimpleEmailServiceConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.SES);
    }

    @Override
    public SimpleEmailServiceConfiguration onCreated(BeanCreatedEvent<SimpleEmailServiceConfiguration> event) {
        SimpleEmailServiceConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.SES).toString());
        return conf;
    }

}
