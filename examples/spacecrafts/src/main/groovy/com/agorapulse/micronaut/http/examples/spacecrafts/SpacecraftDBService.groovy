/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Vladimir Orany.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
