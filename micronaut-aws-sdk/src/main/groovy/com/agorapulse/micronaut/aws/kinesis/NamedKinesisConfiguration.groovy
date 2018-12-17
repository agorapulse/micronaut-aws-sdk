package com.agorapulse.micronaut.aws.kinesis

import com.amazonaws.services.kinesis.AmazonKinesis
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires

@CompileStatic
@EachProperty('aws.kinesis.streams')
@Requires(classes = AmazonKinesis)
class NamedKinesisConfiguration extends KinesisConfiguration {

    final String name

    NamedKinesisConfiguration(@Parameter String name) {
        this.name = name
    }
}
