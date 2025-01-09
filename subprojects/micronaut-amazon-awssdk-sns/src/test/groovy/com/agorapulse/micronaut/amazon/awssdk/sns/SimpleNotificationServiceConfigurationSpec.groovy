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
package com.agorapulse.micronaut.amazon.awssdk.sns

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsClient
import spock.lang.AutoCleanup
import spock.lang.Specification

class SimpleNotificationServiceConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'one service present by default'() {
        when:
        context = ApplicationContext.run()
        then:
        context.getBeanDefinitions(SimpleNotificationServiceConfiguration).size() == 1
        context.getBeanDefinitions(SimpleNotificationService).size() == 1
        context.getBeanDefinitions(SnsClient).size() == 1
        context.getBeanDefinitions(SnsAsyncClient).size() == 1
        context.getBean(SimpleNotificationServiceConfiguration).topic == ''
    }

    void 'configure single service'() {
        when:
        context = ApplicationContext.run(
            'aws.sns.topic': 'mytopic',
            'aws.sns.amazon.arn': 'my-amazon-arn',
            'aws.sns.android.arn': 'my-android-arn',
            'aws.sns.ios.arn': 'my-ios-arn',
            'aws.sns.iosSandbox.arn': 'my-ios-sandbox-arn',
        )
        then:
        context.getBeanDefinitions(SimpleNotificationService).size() == 1
        context.getBean(SimpleNotificationService)
        context.getBean(SnsAsyncClient)

        when:
        SimpleNotificationServiceConfiguration configuration = context.getBean(SimpleNotificationServiceConfiguration)
        then:
        configuration.topic == 'mytopic'
        configuration.amazon.arn == 'my-amazon-arn'
        configuration.android.arn == 'my-android-arn'
        configuration.ios.arn == 'my-ios-arn'
        configuration.iosSandbox.arn == 'my-ios-sandbox-arn'
    }

    void 'configure single named service'() {
        when:
        context = ApplicationContext.run(
            'aws.sns.topics.mytopic.topic': 'mytopic'
        )
        then:
        context.getBeanDefinitions(SimpleNotificationService).size() == 2
        context.getBean(SimpleNotificationService)
        context.getBean(SimpleNotificationService, Qualifiers.byName('mytopic'))
        context.getBean(NamedSimpleNotificationServiceConfiguration).name == 'mytopic'
    }

    void 'configure default and named service'() {
        when:
        context = ApplicationContext.run(
            'aws.sns.topic': 'defaulttopic',
            'aws.sns.topics.mytopic.topic': 'mycustomtopic'
        )
        then:
        context.getBeanDefinitions(SimpleNotificationService).size() == 2
        context.getBean(SimpleNotificationService)
        context.getBean(SimpleNotificationService, Qualifiers.byName('mytopic'))
        context.getBean(SimpleNotificationServiceConfiguration) instanceof DefaultSimpleNotificationServiceConfiguration
        context.getBean(SimpleNotificationServiceConfiguration, Qualifiers.byName('mytopic')) instanceof NamedSimpleNotificationServiceConfiguration
        context.getBean(SimpleNotificationServiceConfiguration, Qualifiers.byName('mytopic')).topic == 'mycustomtopic'
    }

}
