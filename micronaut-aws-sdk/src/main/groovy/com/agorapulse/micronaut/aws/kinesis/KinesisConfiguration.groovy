package com.agorapulse.micronaut.aws.kinesis

import com.amazonaws.services.kinesis.AmazonKinesis
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.NotEmpty

@Named('default')
@CompileStatic
@ConfigurationProperties('aws.kinesis')
@Requires(classes = AmazonKinesis)
class KinesisConfiguration {

    @NotEmpty String stream = ''

    String consumerFilterKey = ''

}
