package com.agorapulse.micronaut.grails

import com.agorapulse.remember.Remember
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

import javax.inject.Named
import javax.inject.Singleton

@Remember(
    value = '2018-11-01',
    description = 'Uncomment tests for combined quaifiers'
)
@ContextConfiguration(classes = [GrailsConfig])
@TestPropertySource("classpath:com/agorapulse/micronaut/grails/GrailsMicronautBeanProcessorSpec.properties")
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
            applicationContext.getBean('prototype') instanceof PrototypeBean
            applicationContext.getBean('someInterface') instanceof SomeInterface
            applicationContext.getBean('someInterface') instanceof SomeImplementation
            applicationContext.getBean('gadget') instanceof SomeGadget
            // see https://github.com/micronaut-projects/micronaut-core/issues/679
            // applicationContext.getBean('otherMinion') instanceof OtherMinion
        when:
            PrototypeBean prototypeBean = applicationContext.getBean(PrototypeBean)
        then:
            prototypeBean.redisHost == REDIS_HOST
            prototypeBean.redisPort == REDIS_PORT
            prototypeBean.redisTimeout == REDIS_TIMEOUT
    }

    void 'cannot preprocess without the environment'() {
        when:
            GrailsMicronautBeanProcessor.builder().build().postProcessBeanFactory(null)
        then:
            thrown(IllegalStateException)
    }

    void 'none-translating environment'() {
        when:
            GrailsPropertyTranslatingEnvironment translatingEnvironment = new GrailsPropertyTranslatingEnvironment(environment, PropertyTranslatingCustomizer.none())
        then:
            !translatingEnvironment.containsProperty('redis.host')
            !translatingEnvironment.getProperty('redis.host', String).present
    }
}

@Configuration
class GrailsConfig {

    @Bean
    GrailsMicronautBeanProcessor widgetProcessor() {
        GrailsMicronautBeanProcessor
            .builder()
            .addByType(Widget)
            .addByType('someInterface', SomeInterface)
            .addByStereotype('prototype', Prototype)
            .addByName('gadget')
            // see https://github.com/micronaut-projects/micronaut-core/issues/679
            // .addByQualifiers('otherMinion', Qualifiers.byName('other'), Qualifiers.byType(Minion))
            .build()
    }

}

interface SomeInterface { }

@Singleton
class SomeImplementation implements SomeInterface { }

@Primary
@Singleton
class Widget {}

@Singleton
@Requires(notEnv = 'test')
class TestWidget extends Widget { }


interface Minion {}

@Prototype
class PrototypeBean {

    final String redisHost
    final Integer redisPort
    final Integer redisTimeout

    PrototypeBean(
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
class OtherMinion implements Minion {}

@Singleton
class NormalMinion implements Minion {}
