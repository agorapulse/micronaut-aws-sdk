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
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.inject.qualifiers.Qualifiers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

import javax.inject.Named
import javax.inject.Scope
import javax.inject.Singleton
import java.lang.annotation.Documented
import java.lang.annotation.Retention

import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * Tests for micronaut Spring bean processor.
 */
@ContextConfiguration(classes = [GrailsConfig, MicronautGrailsConfiguration])
@TestPropertySource('classpath:com/agorapulse/micronaut/grails/GrailsMicronautBeanProcessorSpec.properties')
class GrailsMicronautBeanProcessorSpec extends Specification {

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
            CustomBean customScopeBean = applicationContext.getBean(CustomBean)
        then:
            customScopeBean.redisHost == REDIS_HOST
            customScopeBean.redisPort == REDIS_PORT
            customScopeBean.redisTimeout == REDIS_TIMEOUT
    }

    void 'cannot preprocess without the environment'() {
        when:
            GrailsMicronautBeanProcessor.builder().build().postProcessBeanFactory(null)
        then:
            thrown(IllegalStateException)
    }

    void 'none-translating environment'() {
        when:
            GrailsPropertyTranslatingEnvironment translatingEnvironment = new GrailsPropertyTranslatingEnvironment(
                environment,
                PropertyTranslatingCustomizer.none()
            )
        then:
            !translatingEnvironment.containsProperty('redis.host')
            !translatingEnvironment.getProperty('redis.host', String).present
    }
}

// tag::configuration[]
@CompileStatic
@Configuration                                                                          // <1>
class GrailsConfig {

    @Bean
    MicronautBeanImporter myImporter() {                                                // <2>
        MicronautBeanImporter.create()
            .addByType(Widget)                                                          // <3>
            .addByType('someInterface', SomeInterface)                                  // <4>
            .addByStereotype('custom', SomeCustomScope)                                 // <5>
            .addByName('gadget')                                                        // <6>
            .addByName('one')
            .addByName('two')
            .addByQualifiers(                                                           // <7>
                'otherMinion',
                Qualifiers.byName('other'),
                Qualifiers.byType(Minion)
            )
    }

}
// end::configuration[]

interface SomeInterface { }

@Singleton
class SomeImplementation implements SomeInterface { }

class SomeNamed {
    final String name

    SomeNamed(String name) {
        this.name = name
    }
}

@Factory
class SomeNamedFactory {

    @io.micronaut.context.annotation.Bean
    @Singleton
    @Named('one')
    SomeNamed one() {
        return new SomeNamed('one')
    }

    @io.micronaut.context.annotation.Bean
    @Singleton
    @Named('two')
    SomeNamed two() {
        return new SomeNamed('two')
    }

}

@Primary
@Singleton
class Widget { }

@Singleton
@Requires(notEnv = 'test')
class TestWidget extends Widget { }

interface Minion { }

@Documented
@Retention(RUNTIME)
@Scope
@io.micronaut.context.annotation.Bean
@interface SomeCustomScope { }

@SomeCustomScope
class CustomBean {

    final String redisHost
    final Integer redisPort
    final Integer redisTimeout

    CustomBean(
        @Value('${redis.host}') String redisHost,
        @Value('${redis.port}') Integer redisPort,
        @Value('${redis.timeout:10000}') Integer redisTimeout
    ) {
        this.redisHost = redisHost
        this.redisPort = redisPort
        this.redisTimeout = redisTimeout
    }
}

@Singleton
@Named('gadget')
class SomeGadget { }

@Singleton
@Named('other')
class OtherMinion implements Minion { }

@Singleton
class NormalMinion implements Minion { }
