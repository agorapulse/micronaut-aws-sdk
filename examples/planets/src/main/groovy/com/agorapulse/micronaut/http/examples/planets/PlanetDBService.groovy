package com.agorapulse.micronaut.http.examples.planets

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper

import javax.inject.Singleton

@Singleton
class PlanetDBService {

    IDynamoDBMapper mapper = new DynamoDBMapper(new AmazonDynamoDBClient())

    void save(Planet planet) {
        mapper.save(planet)
    }

    void delete(Planet planet) {
        mapper.delete(planet)
    }

    List<Planet> findAllByStar(String starName) {
        mapper.query(Planet, new DynamoDBQueryExpression<Planet>().withHashKeyValues(new Planet(star: starName)))
    }

    Planet get(String starName, String planetName) {
        mapper.load(Planet, starName, planetName)
    }

}
