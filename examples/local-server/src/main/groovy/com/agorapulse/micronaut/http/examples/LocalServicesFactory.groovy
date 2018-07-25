package com.agorapulse.micronaut.http.examples

import com.agorapulse.dru.DataSet
import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.agp.MockContext
import com.agorapulse.micronaut.http.examples.planets.Planet
import com.agorapulse.micronaut.http.examples.planets.PlanetDBService
import com.agorapulse.micronaut.http.examples.spacecrafts.Spacecraft
import com.agorapulse.micronaut.http.examples.spacecrafts.SpacecraftDBService
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.lambda.runtime.Context
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.retry.annotation.Fallback

import javax.annotation.PostConstruct
import javax.inject.Singleton

@Factory
@CompileStatic
class LocalServicesFactory {

    private final DataSet dataSet = Dru.steal(this)
    private final DynamoDBMapper mapper = DynamoDB.createMapper(dataSet)

    @PostConstruct
    void bootstrap() {
        dataSet.add(new Planet(star: 'sun', name: 'mercury'))
        dataSet.add(new Planet(star: 'sun', name: 'venus'))
        dataSet.add(new Planet(star: 'sun', name: 'earth'))
        dataSet.add(new Planet(star: 'sun', name: 'mars'))
        dataSet.add(new Planet(star: 'sun', name: 'jupiter'))
        dataSet.add(new Planet(star: 'sun', name: 'saturn'))
        dataSet.add(new Planet(star: 'sun', name: 'uranus'))
        dataSet.add(new Planet(star: 'sun', name: 'neptune'))

        dataSet.add(new Spacecraft(country: 'russia', name: 'vostok'))
        dataSet.add(new Spacecraft(country: 'usa', name: 'dragon'))
    }

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
