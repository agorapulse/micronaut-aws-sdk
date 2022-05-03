package com.agorapulse.micronaut.amazon.awssdk.localstack.v2.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisFactory;
import com.agorapulse.micronaut.amazon.awssdk.localstack.LocalstackContainerHolder;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.core.SdkSystemSetting;

import javax.inject.Singleton;
import java.io.Closeable;

@Singleton
@Requires(classes = KinesisFactory.class, beans = LocalstackContainerHolder.class)
public class KinesisConfigurationListener implements BeanCreatedEventListener<KinesisConfiguration>, Closeable {

    private final LocalstackContainerHolder holder;
    private final String oldCborValue;

    public KinesisConfigurationListener(LocalstackContainerHolder holder) {
        this.holder = holder.withServiceEnabled(LocalStackContainer.Service.KINESIS);
        this.oldCborValue = System.getProperty(SdkSystemSetting.CBOR_ENABLED.property());
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), "false");
    }

    @Override
    public KinesisConfiguration onCreated(BeanCreatedEvent<KinesisConfiguration> event) {
        KinesisConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(LocalStackContainer.Service.KINESIS).toString());
        return conf;
    }

    @Override
    public void close() {
        System.setProperty(SdkSystemSetting.CBOR_ENABLED.property(), oldCborValue);
    }

}
