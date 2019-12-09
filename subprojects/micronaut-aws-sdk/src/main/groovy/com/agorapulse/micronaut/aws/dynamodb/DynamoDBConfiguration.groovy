package com.agorapulse.micronaut.aws.dynamodb

import com.agorapulse.micronaut.aws.DefaultRegionAndEndpointConfiguration
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDB
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires

import javax.inject.Named

/**
 * Default simple storage service configuration.
 */
@Named('default')
@CompileStatic
@ConfigurationProperties('aws.dynamodb')
@Requires(classes = DynamoDB)
class DynamoDBConfiguration extends DefaultRegionAndEndpointConfiguration {

}
