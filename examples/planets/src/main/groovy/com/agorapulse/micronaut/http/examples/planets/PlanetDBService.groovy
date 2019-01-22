package com.agorapulse.micronaut.http.examples.planets

import com.agorapulse.micronaut.aws.dynamodb.annotation.HashKey
import com.agorapulse.micronaut.aws.dynamodb.annotation.Query
import com.agorapulse.micronaut.aws.dynamodb.annotation.RangeKey
import com.agorapulse.micronaut.aws.dynamodb.annotation.Service
import com.agorapulse.micronaut.aws.dynamodb.builder.Builders

/**
 * Service to access DynamoDB entities.
 */
@Service(Planet)
interface PlanetDBService {

    void save(Planet planet)
    void delete(Planet planet)

    @Query({
        Builders.query(Planet) {
            hash starName
        }
    })
    List<Planet> findAllByStar(String starName)
    Planet get(@HashKey String starName, @RangeKey String planetName)

}
