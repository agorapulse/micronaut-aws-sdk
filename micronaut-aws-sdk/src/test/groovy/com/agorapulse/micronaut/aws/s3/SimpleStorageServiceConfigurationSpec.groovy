package com.agorapulse.micronaut.aws.s3

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.AutoCleanup
import spock.lang.Specification

class SimpleStorageServiceConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context = null

    void 'no service present by default'() {
        when:
            context = ApplicationContext.run()
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 0
    }

    void 'configure single service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.bucket': 'bucket.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 1
            context.getBean(SimpleStorageService)
    }

    void 'configure single named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.buckets.samplebucket.bucket': 'bucket.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 1
            context.getBean(SimpleStorageService)
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
    }

    void 'configure default and named service'() {
        when:
            context = ApplicationContext.run(
                'aws.s3.bucket': 'default.example.com',
                'aws.s3.buckets.samplebucket.bucket': 'sample.example.com'
            )
        then:
            context.getBeanDefinitions(SimpleStorageService).size() == 2
            context.getBean(SimpleStorageService, Qualifiers.byName('default'))
            context.getBean(SimpleStorageService, Qualifiers.byName('samplebucket'))
    }

}
