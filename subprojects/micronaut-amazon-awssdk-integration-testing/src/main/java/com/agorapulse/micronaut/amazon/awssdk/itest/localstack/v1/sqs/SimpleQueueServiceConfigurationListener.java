/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2025 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack.v1.sqs;

import com.agorapulse.micronaut.amazon.awssdk.itest.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.aws.sqs.SimpleQueueServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import jakarta.inject.Singleton;

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
