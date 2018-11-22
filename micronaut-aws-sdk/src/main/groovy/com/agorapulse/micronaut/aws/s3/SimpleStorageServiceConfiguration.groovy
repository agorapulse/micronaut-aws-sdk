package com.agorapulse.micronaut.aws.s3

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter

import javax.validation.constraints.NotEmpty

@CompileStatic
@EachProperty('aws.s3.buckets')
class SimpleStorageServiceConfiguration {

    final String name

    @NotEmpty String bucket = ''

    SimpleStorageServiceConfiguration(@Parameter String name) {
        this.name = name
    }
}
