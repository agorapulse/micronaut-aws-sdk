package com.agorapulse.micronaut.http.examples.spacecrafts

import com.agorapulse.micronaut.aws.dynamodb.annotation.HashKey
import com.agorapulse.micronaut.aws.dynamodb.annotation.Query
import com.agorapulse.micronaut.aws.dynamodb.annotation.RangeKey
import com.agorapulse.micronaut.aws.dynamodb.annotation.Service
import com.agorapulse.micronaut.aws.dynamodb.builder.Builders

/**
 * Spacecraft entity DynamoDB service.
 */
@Service(Spacecraft)
interface SpacecraftDBService {

    void save(Spacecraft spacecraft)

    void delete(Spacecraft spacecraft)

    @Query({
        Builders.query(Spacecraft) {
            hash countryName
        }
    })
    List<Spacecraft> findAllByCountry(String countryName)

    Spacecraft get(@HashKey String countryName, @RangeKey String planetName)

}
