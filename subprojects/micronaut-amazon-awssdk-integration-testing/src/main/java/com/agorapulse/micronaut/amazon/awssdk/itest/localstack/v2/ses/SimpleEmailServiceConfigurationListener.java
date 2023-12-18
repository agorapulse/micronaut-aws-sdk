/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2023 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.localstack.v2.ses;

import com.agorapulse.micronaut.amazon.awssdk.itest.localstack.LocalstackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.ses.SimpleEmailServiceConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import org.testcontainers.containers.localstack.LocalStackContainer;

import jakarta.inject.Singleton;

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
