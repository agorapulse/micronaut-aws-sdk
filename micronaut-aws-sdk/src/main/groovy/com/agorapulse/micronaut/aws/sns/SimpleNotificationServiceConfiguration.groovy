package com.agorapulse.micronaut.aws.sns

import com.amazonaws.services.sns.AmazonSNS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.NotEmpty

@Named('default')
@CompileStatic
@ConfigurationProperties('aws.sns')
@Requires(classes = AmazonSNS.class)
class SimpleNotificationServiceConfiguration {

    static class Application {
        @NotEmpty String arn
    }

    String topic = ''

    Application ios = new Application()
    Application android = new Application()
    Application amazon = new Application()

}
