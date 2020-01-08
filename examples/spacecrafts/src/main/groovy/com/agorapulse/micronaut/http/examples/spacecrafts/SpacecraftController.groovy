/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status

/**
 * Spacecraft controller.
 */
@CompileStatic
@Controller('/spacecraft')
class SpacecraftController {

    private final SpacecraftDBService spacecraftDBService

    SpacecraftController(SpacecraftDBService spacecraftDBService) {
        this.spacecraftDBService = spacecraftDBService
    }

    @Get('/{country}')
    List<Spacecraft> list(String country) {
        return spacecraftDBService.findAllByCountry(country)
    }

    @Get('/{country}/{name}')
    Spacecraft show(String country, String name) {
        Spacecraft spacecraft = spacecraftDBService.get(country, name)

        if (!spacecraft) {
            throw new SpacecraftNotFoundException(name)
        }

        return spacecraft
    }

    @Post('/{country}/{name}')
    @Status(HttpStatus.CREATED)
    Spacecraft save(String country, String name) {
        Spacecraft planet = new Spacecraft(country: country, name: name)
        spacecraftDBService.save(planet)
        return planet
    }

    @Delete('/{country}/{name}')
    @Status(HttpStatus.NO_CONTENT)
    Spacecraft delete(String country, String name) {
        Spacecraft planet = show(country, name)
        spacecraftDBService.delete(planet)
        return planet
    }

    @Status(HttpStatus.NOT_FOUND)
    @Error(SpacecraftNotFoundException)
    @SuppressWarnings([
        'EmptyMethod',
        'UnusedMethodParameter',
    ])
    void spacecraftNotFound(SpacecraftNotFoundException ex) { }

}
