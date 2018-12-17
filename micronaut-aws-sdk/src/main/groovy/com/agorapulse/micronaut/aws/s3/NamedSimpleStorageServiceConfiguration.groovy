package com.agorapulse.micronaut.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs.AmazonSQS
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

import javax.validation.constraints.NotEmpty

@CompileStatic
@EachProperty('aws.s3.buckets')
@Requires(classes = AmazonS3)
class NamedSimpleStorageServiceConfiguration extends SimpleStorageServiceConfiguration {

    final String name

    NamedSimpleStorageServiceConfiguration(@Parameter String name) {
        this.name = name
    }
}
