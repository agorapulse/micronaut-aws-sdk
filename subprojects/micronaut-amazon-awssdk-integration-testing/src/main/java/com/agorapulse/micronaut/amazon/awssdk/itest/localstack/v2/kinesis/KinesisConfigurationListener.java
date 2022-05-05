/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack.v2.kinesis;

import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.kinesis.KinesisFactory;
import com.agorapulse.micronaut.amazon.awssdk.itest.localstack.LocalstackContainerHolder;
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
