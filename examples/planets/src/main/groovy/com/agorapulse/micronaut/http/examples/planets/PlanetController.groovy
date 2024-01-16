/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2024 Agorapulse.
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
package com.agorapulse.micronaut.http.examples.planets

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status

/**
 * Planet controller.
 */
@CompileStatic
@Controller('/planet')
class PlanetController {

    private final PlanetDBService planetDBService

    PlanetController(PlanetDBService planetDBService) {
        this.planetDBService = planetDBService
    }

    @Get('/{star}')
    List<Planet> list(String star) {
        return planetDBService.findAllByStar(star)
    }

    @Get('/{star}/{name}')
    Planet show(String star, String name) {
        Planet planet = planetDBService.get(star, name)
        if (!planet) {
            throw new PlanetNotFoundException(name)
        }
        return planet
    }

    @Post('/{star}/{name}') @Status(HttpStatus.CREATED)
    Planet save(String star, String name) {
        Planet planet = new Planet(star: star, name: name)
        planetDBService.save(planet)
        return planet
    }

    @Delete('/{star}/{name}') @Status(HttpStatus.NO_CONTENT)
    Planet delete(String star, String name) {
        Planet planet = show(star, name)
        planetDBService.delete(planet)
        return planet
    }

    @Status(HttpStatus.NOT_FOUND)
    @Error(PlanetNotFoundException)
    @SuppressWarnings([
        'EmptyMethod',
        'UnusedMethodParameter',
    ])
    void planetNotFound(PlanetNotFoundException ex) { }

}
