package com.agorapulse.micronaut.http.examples.spacecrafts

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper

import javax.inject.Singleton

@Singleton
class SpacecraftDBService {

    IDynamoDBMapper mapper = new DynamoDBMapper(new AmazonDynamoDBClient())

    void save(Spacecraft spacecraft) {
        mapper.save(spacecraft)
    }

    void delete(Spacecraft spacecraft) {
        mapper.delete(spacecraft)
    }

    List<Spacecraft> findAllByCountry(String countryName) {
        mapper.query(Spacecraft, new DynamoDBQueryExpression<Spacecraft>().withHashKeyValues(new Spacecraft(country: countryName)))
    }

    Spacecraft get(String countryName, String planetName) {
        mapper.load(Spacecraft, countryName, planetName)
    }

}
