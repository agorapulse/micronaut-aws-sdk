/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2026 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.itest.ministack.v2.dynamodb;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBClientsFactory;
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.DynamoDBConfiguration;
import com.agorapulse.micronaut.amazon.awssdk.itest.ministack.MinistackContainerHolder;
import com.agorapulse.micronaut.amazon.awssdk.itest.ministack.MinistackService;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import jakarta.inject.Singleton;

@Singleton
@Requires(classes = DynamoDBClientsFactory.class, beans = MinistackContainerHolder.class)
public class DynamoDBConfigurationListener implements BeanCreatedEventListener<DynamoDBConfiguration> {

    private final MinistackContainerHolder holder;

    public DynamoDBConfigurationListener(MinistackContainerHolder holder) {
        this.holder = holder;
    }

    @Override
    public DynamoDBConfiguration onCreated(BeanCreatedEvent<DynamoDBConfiguration> event) {
        DynamoDBConfiguration conf = event.getBean();
        if (conf.getEndpoint() != null) {
            return conf;
        }
        conf.setEndpoint(holder.getEndpointOverride(MinistackService.DYNAMODB).toString());
        return conf;
    }

}
