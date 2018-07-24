package com.agorapulse.micronaut.http.examples

import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.agp.MockContext
import com.agorapulse.micronaut.http.examples.planets.PlanetDBService
import com.agorapulse.micronaut.http.examples.spacecrafts.SpacecraftDBService
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.lambda.runtime.Context
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.retry.annotation.Fallback

import javax.inject.Singleton

@Factory
@CompileStatic
class LocalServicesFactory {

    DynamoDBMapper mapper = DynamoDB.createMapper(Dru.steal(this))

    @Bean
    @Fallback
    @Singleton
    Context context() {
        return new MockContext()
    }

    @Bean
    @Primary
    @Singleton
    PlanetDBService planetDBService() {
        return new PlanetDBService(mapper: mapper)
    }

    @Bean
    @Primary
    @Singleton
    SpacecraftDBService spacecraftDBService() {
        return new SpacecraftDBService(mapper: mapper)
    }

}
