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
package com.agorapulse.micronaut.amazon.awssdk.core.client;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

import java.time.Duration;

// tag::customizer[]
@Singleton
public class NettyClientCustomizer implements BeanCreatedEventListener<NettyNioAsyncHttpClient.Builder> {

    @Override
    public NettyNioAsyncHttpClient.Builder onCreated(@NonNull BeanCreatedEvent<NettyNioAsyncHttpClient.Builder> event) {
        event.getBean().readTimeout(Duration.ofSeconds(10));
        return event.getBean();
    }

}
// end::customizer[]
