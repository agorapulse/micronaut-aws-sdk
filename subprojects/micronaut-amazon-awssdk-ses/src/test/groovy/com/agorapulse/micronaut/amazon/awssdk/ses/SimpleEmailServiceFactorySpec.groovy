/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2021 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.ses

import io.micronaut.context.ApplicationContext
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.SesClient
import spock.lang.AutoCleanup
import spock.lang.Specification

class SimpleEmailServiceFactorySpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'definitions are present'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(SimpleEmailServiceConfiguration).size() == 1
            context.getBeanDefinitions(SimpleEmailService).size() == 1
            context.getBeanDefinitions(SesClient).size() == 1
            context.getBeanDefinitions(SesAsyncClient).size() == 1
    }

    void 'beans can constructed'() {
        when:
        context = ApplicationContext.run()
        then:
        context.getBean(SimpleEmailServiceConfiguration)
        context.getBean(SimpleEmailService)
        context.getBean(SesClient)
        context.getBean(SesAsyncClient)
    }

}
