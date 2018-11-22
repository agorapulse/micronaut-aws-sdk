package com.agorapulse.micronaut.aws.s3

import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named
import javax.validation.constraints.NotEmpty

@Named('default')
@CompileStatic
@ConfigurationProperties('aws.s3')
@Requires(classes = AmazonSQS)
class SimpleStorageServiceConfiguration {

    @NotEmpty String bucket = ''

}
