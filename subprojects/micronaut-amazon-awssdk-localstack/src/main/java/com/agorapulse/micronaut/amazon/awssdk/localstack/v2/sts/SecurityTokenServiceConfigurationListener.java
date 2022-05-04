package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.sts;

import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.sts.SecurityTokenServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import javax.inject.Singleton;

@Singleton
@Requires(classes = SecurityTokenServiceConfiguration.class, beans = LocalstackContainerHolder.class)
public class SecurityTokenServiceConfigurationListener implements BeanCreatedEventListener<SecurityTokenServiceConfiguration> {

    private final LocalstackContainerHolder holder;

    public SecurityTokenServiceConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.STS);
    }

    @Override
    public SecurityTokenServiceConfiguration onCreated(BeanCreatedEvent<SecurityTokenServiceConfiguration> event) {
        SecurityTokenServiceConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.STS).toString());
        return conf;
    }

}
