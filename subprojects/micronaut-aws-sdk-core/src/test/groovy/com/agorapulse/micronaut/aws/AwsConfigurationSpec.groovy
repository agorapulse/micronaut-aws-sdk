package com.agorapulse.micronaut.aws

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.AwsRegionProvider
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

class AwsConfigurationSpec extends Specification {

    @AutoCleanup ApplicationContext context

    void setup() {
        context = ApplicationContext.run(
            'aws.cloudwatch.region': 'eu-west-1'
        )
    }

    void 'check configuration present'() {
        expect:
            context.getBean(AWSCredentialsProvider)
            context.getBean(AwsRegionProvider)
    }

}
