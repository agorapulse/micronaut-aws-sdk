package com.agorapulse.micronaut.grails


import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

import javax.inject.Singleton

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
            applicationContext.getBean('prototypeBean') instanceof PrototypeBean
        when:
            PrototypeBean prototypeBean = applicationContext.getBean(PrototypeBean)
        then:
            prototypeBean.redisHost == REDIS_HOST
            prototypeBean.redisPort == REDIS_PORT
            prototypeBean.redisTimeout == REDIS_TIMEOUT
    }

    void 'cannot preprocess without the environment'() {
        when:
            new GrailsMicronautBeanProcessor().postProcessBeanFactory(null)
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
        new GrailsMicronautBeanProcessor(Widget, Prototype)
    }

}

@Singleton
class Widget {}

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
