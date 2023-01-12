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
package com.agorapulse.micronaut.aws.sns

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import groovy.transform.CompileStatic
import io.micronaut.context.env.Environment

/**
 * Default simple queue service configuration.
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class SimpleNotificationServiceConfiguration extends DefaultRegionAndEndpointConfiguration {

    static class Application {

        final String arn

        Application(String arn) {
            this.arn = arn
        }

    }

    private final Environment environment

    final Application adm
    final Application apns
    final Application apnsSandbox
    final Application gcm

    @Deprecated
    Application getIos() { return apns }

    @Deprecated
    Application getIosSandbox() { return apnsSandbox }

    @Deprecated
    Application getAndroid() { return gcm }

    @Deprecated
    Application getAmazon() { return adm }

    String topic = ''

    protected SimpleNotificationServiceConfiguration(String prefix, Environment environment) {
        this.environment = environment
        adm = forPlatform(prefix, 'adm', 'amazon', environment)
        apns = forPlatform(prefix, 'apns', 'ios', environment)
        apnsSandbox = forPlatform(prefix, 'apnsSandbox', 'iosSandbox', environment)
        gcm = forPlatform(prefix, 'gcm', 'android', environment)
    }

    @SuppressWarnings('DuplicateStringLiteral')
    private static Application forPlatform(String prefix, String platform, String fallbackPlatform, Environment environment) {
        return new Application(
            environment.get(prefix + '.' + platform + '.arn', String).orElseGet {
                environment.get(prefix + '.' + platform + '.applicationArn', String).orElseGet {
                    environment.get(prefix + '.' + fallbackPlatform + '.arn', String).orElseGet {
                        environment.get(prefix + '.' + fallbackPlatform + '.applicationArn', String).orElse(null)
                    }
                }
            })
    }

}
