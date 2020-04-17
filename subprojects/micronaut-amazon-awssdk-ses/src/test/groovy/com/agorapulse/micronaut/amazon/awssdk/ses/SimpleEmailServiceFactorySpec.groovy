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
