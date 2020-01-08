package com.agorapulse.micronaut.aws.cloudwatch

import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

class CloudWatchFactorySpec extends Specification {

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.run(
            'aws.cloudwatch.region': 'eu-west-1'
        )
    }

    void 'check configuration present'() {
        expect:
            context.getBean(CloudWatchConfiguration)
    }

}
