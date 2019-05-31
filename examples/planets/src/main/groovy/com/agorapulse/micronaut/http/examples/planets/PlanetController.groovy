package com.agorapulse.micronaut.http.examples.planets

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
    void planetNotFound(PlanetNotFoundException ex) { }

}
