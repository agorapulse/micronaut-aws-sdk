/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
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
package com.agorapulse.micronaut.grails

import groovy.transform.CompileStatic
import io.micronaut.inject.qualifiers.Qualifiers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

/**
 * Tests for micronaut Spring bean processor.
 */
@ContextConfiguration(classes = [GrailsLegacyConfig])
@TestPropertySource('classpath:com/agorapulse/micronaut/grails/GrailsMicronautBeanProcessorSpec.properties')
class LegacyGrailsMicronautBeanProcessorSpec extends Specification {

    public static final int REDIS_PORT = 12345
    public static final int REDIS_TIMEOUT = 10000
    public static final String REDIS_HOST = 'localhost'

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    Environment environment

    void 'test widget bean'() {
        expect:
            applicationContext.getBean('widget') instanceof Widget
            applicationContext.getBean('custom') instanceof CustomBean
            applicationContext.getBean('someInterface') instanceof SomeInterface
            applicationContext.getBean('someInterface') instanceof SomeImplementation
            applicationContext.getBean('gadget') instanceof SomeGadget

            applicationContext.getBean('two') instanceof SomeNamed
            applicationContext.getBean('one') instanceof SomeNamed

            applicationContext.getBean('one').name == 'one'
            applicationContext.getBean('two').name == 'two'

            applicationContext.getBean('otherMinion') instanceof OtherMinion
        when:
            CustomBean prototypeBean = applicationContext.getBean(CustomBean)
        then:
            prototypeBean.redisHost == REDIS_HOST
            prototypeBean.redisPort == REDIS_PORT
            prototypeBean.redisTimeout == REDIS_TIMEOUT
    }
}

// tag::configuration[]
@CompileStatic
@Configuration
class GrailsLegacyConfig {

    @Bean
    GrailsMicronautBeanProcessor widgetProcessor() {
        GrailsMicronautBeanProcessor
            .builder()
            .addByType(Widget)
            .addByType('someInterface', SomeInterface)
            .addByStereotype('custom', SomeCustomScope)
            .addByName('gadget')
            .addByName('one')
            .addByName('two')
            .addByQualifiers(
                'otherMinion',
                Qualifiers.byName('other'),
                Qualifiers.byType(Minion)
            )
            .build()
    }

}
