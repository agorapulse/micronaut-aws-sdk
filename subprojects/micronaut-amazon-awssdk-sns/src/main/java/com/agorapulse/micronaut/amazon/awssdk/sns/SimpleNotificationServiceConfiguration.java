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
package com.agorapulse.micronaut.amazon.awssdk.sns;

import com.agorapulse.micronaut.amazon.awssdk.core.DefaultRegionAndEndpointConfiguration;
import io.micronaut.context.env.Environment;

/**
 * Default simple queue service configuration.
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethod")
public abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    protected SimpleNotificationServiceConfiguration(String prefix, Environment environment) {
        apns = forPlatform(prefix, "apns", "ios", environment);
        apnsSandbox = forPlatform(prefix, "apns-sandbox", "ios-sandbox", environment);
        gcm = forPlatform(prefix, "gcm", "android", environment);
        adm = forPlatform(prefix, "adm", "amazon", environment);
    }

    @SuppressWarnings("DuplicateStringLiteral")
    private static Application forPlatform(final String prefix, final String platform, final String fallbackPlatform, final Environment environment) {
        return new Application(
            environment.get(prefix + "." + platform + ".arn", String.class).orElseGet(() ->
                environment.get(prefix + "." + platform + ".applicationArn", String.class).orElseGet(() ->
                    environment.get(prefix + "." + fallbackPlatform + ".arn", String.class).orElseGet(() ->
                        environment.get(prefix + "." + fallbackPlatform + ".applicationArn", String.class).orElse(null)
                    )
                )
            )
        );
    }

    @Deprecated
    public final Application getIos() {
        return apns;
    }

    @Deprecated
    public final Application getIosSandbox() {
        return apnsSandbox;
    }

    @Deprecated
    public final Application getAndroid() {
        return gcm;
    }

    @Deprecated
    public final Application getAmazon() {
        return adm;
    }

    public Application getApns() {
        return apns;
    }

    public Application getApnsSandbox() {
        return apnsSandbox;
    }

    public Application getGcm() {
        return gcm;
    }

    public Application getAdm() {
        return adm;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    private final Application apns;
    private final Application apnsSandbox;
    private final Application gcm;
    private final Application adm;
    private String topic = "";

    public static class Application {

        public Application(String arn) {
            this.arn = arn;
        }

        public final String getArn() {
            return arn;
        }

        private final String arn;

    }
}
