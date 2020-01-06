/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Vladimir Orany.
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
package com.agorapulse.micronaut.http.examples

import com.agorapulse.dru.DataSet
import com.agorapulse.dru.Dru
import com.agorapulse.dru.dynamodb.persistence.DynamoDB
import com.agorapulse.gru.agp.MockContext
import com.agorapulse.micronaut.http.examples.planets.Planet
import com.agorapulse.micronaut.http.examples.spacecrafts.Spacecraft
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper
import com.amazonaws.services.lambda.runtime.Context
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.retry.annotation.Fallback

import javax.annotation.PostConstruct
import javax.inject.Singleton

/**
 * Mock services for the example application.
 */
@Factory
@CompileStatic
class LocalServicesFactory {

    private static final String SUN = 'sun'

    private final DataSet dataSet = Dru.steal(this)
    private final DynamoDBMapper mapper = DynamoDB.createMapper(dataSet)

    @PostConstruct
    void bootstrap() {
        dataSet.with {
            add(new Planet(star: SUN, name: 'mercury'))
            add(new Planet(star: SUN, name: 'venus'))
            add(new Planet(star: SUN, name: 'earth'))
            add(new Planet(star: SUN, name: 'mars'))
            add(new Planet(star: SUN, name: 'jupiter'))
            add(new Planet(star: SUN, name: 'saturn'))
            add(new Planet(star: SUN, name: 'uranus'))
            add(new Planet(star: SUN, name: 'neptune'))
            add(new Spacecraft(country: 'russia', name: 'vostok'))
            add(new Spacecraft(country: 'usa', name: 'dragon'))
        }
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
    IDynamoDBMapper dynamoDBMapper() {
        mapper
    }

}
